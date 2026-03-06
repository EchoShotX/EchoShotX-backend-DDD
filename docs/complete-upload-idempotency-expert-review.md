# Complete Upload 멱등성/중복 방지 아키텍처 최종 검토서

## 0. 문서 목적

이 문서는 `POST /videos/{videoId}/complete-upload`의 중복 요청(더블 클릭, 네트워크 재시도, 멀티 인스턴스 동시 처리) 문제에 대해,

- 왜 특정 기술 조합을 채택해야 하는지,
- 다른 대안은 왜 단독으로 부족한지,
- 엣지 케이스에서 어떻게 실패하지 않는지,
- 기술 면접/아키텍처 리뷰에서 받는 날카로운 질문까지

논리적으로 방어 가능한 수준으로 정리한 최종 의사결정 자료입니다.

---

## 1. 결론 (Executive Decision)

최종 권장안은 다음 4요소 결합입니다.

1. `Redis 분산락` (진입 경합 완화)
2. `DB 유니크 제약` (중복 생성 최종 차단)
3. `멱등 응답 계약` (재시도 요청 동일 결과 수렴)
4. `커밋 후 발행` (`AFTER_COMMIT` 또는 Outbox) (상태-메시지 정합성)

핵심 이유:
- 단일 기술로는 "정합성 + 성능 + 운영경험"을 동시에 만족할 수 없습니다.
- 이 4요소는 역할이 겹치지 않고, 각자의 실패 모드를 상호 보완합니다.

---

## 2. 현행 코드 기준 진단

참조 위치:
- `src/main/java/com/example/echoshotx/video/application/usecase/CompleteVideoUploadUseCase.java`
- `src/main/java/com/example/echoshotx/video/infrastructure/persistence/VideoRepository.java`
- `src/main/java/com/example/echoshotx/job/infrastructure/persistence/JobRepository.java`
- `src/main/java/com/example/echoshotx/job/application/handler/JobEventHandler.java`
- `src/main/java/com/example/echoshotx/credit/application/service/CreditService.java`

현행의 장점:
- `PESSIMISTIC_WRITE`로 동시 진입 제어 효과는 확인됨

현행의 구조적 취약점:
1. `job.video_id` 중복 차단 DB 제약 부재
2. `@TransactionalEventListener`가 붙은 핸들러를 직접 호출하는 구조(이벤트 발행이 아님)
3. 재시도 요청에 대한 API 멱등 계약 부재(`Idempotency-Key` 없음)
4. 중복 요청 시 실패 응답 중심이라 운영/UX 비용 증가

---

## 3. 문제를 정확히 정의한 불변식 (Invariants)

이 API에서 절대 깨지면 안 되는 불변식:

- I1. 동일 `videoId`에 대해 활성 Job은 최대 1개
- I2. 동일 업로드 완료 요청으로 크레딧이 2번 이상 차감되지 않음
- I3. DB 트랜잭션 롤백 시 외부 큐 발행 결과가 앞서 나가지 않음
- I4. 동일 요청 재전송은 실패 폭탄이 아니라 동일 결과로 수렴

평가 기준:
- Correctness(정합성), Availability(가용성), Operability(운영성), Performance(성능), Simplicity(단순성)

---

## 4. 대안 비교: 왜 이 조합이어야 하는가

## 4.1 단일 기술 대안의 한계

### A. 비관적 락만 사용

장점:
- 동시 실행 직렬화

한계:
- 락 획득/해제와 무관한 중복 삽입 버그를 DB가 막지 못함
- 고경합에서 대기/타임아웃 증가
- API 멱등 계약 부재

판정: "1차 방어선"으로는 적합, "최종 설계"로는 미흡

### B. Redis 락만 사용

장점:
- 앱 레벨에서 빠른 경합 완화

한계:
- 네트워크 분할/락 만료/프로세스 중단 시 정합성 단독 보장 불가
- 데이터 무결성은 결국 DB 제약이 담당해야 함

판정: 성능 장치로 유효, 무결성 장치로 불충분

### C. DB 유니크만 사용

장점:
- 정합성 가장 강함

한계:
- 경합 시 예외 폭증, 사용자 체감 실패 증가
- 재시도 요청의 UX/계약 문제는 해결하지 못함

판정: 필수이나 단독으론 운영 경험 불완전

### D. Idempotency-Key만 사용

장점:
- API 재시도 수렴

한계:
- 내부에서 실제 중복 삽입 방지가 없으면 무의미

판정: 계약 계층으로 필수, 물리적 제약 대체 불가

## 4.2 결론: 다층 방어가 정답

- Redis: "경합 완충"
- DB Unique: "정합성 최종 보루"
- Idempotent Response: "재시도 수렴"
- After-Commit Publish: "비동기 정합성"

이 구성은 실패 모드를 분리하고, 한 층이 실패해도 다른 층이 붕괴를 막습니다.

---

## 5. 기술 선택 근거 (의사결정 매트릭스)

점수: 1(낮음) ~ 5(높음)

| 옵션 | 정합성 | 성능 | 운영성 | 구현난이도 | 총평 |
|---|---:|---:|---:|---:|---|
| 비관적 락 단독 | 3 | 2 | 3 | 4 | 단기 응급처치 |
| Redis 락 단독 | 2 | 4 | 3 | 3 | 성능은 좋으나 무결성 약함 |
| DB 유니크 단독 | 5 | 3 | 2 | 4 | 정합성은 강하나 UX 거칠음 |
| Idempotency-Key 단독 | 2 | 4 | 4 | 3 | 계약은 좋으나 최종 차단 부재 |
| Redis+DB+멱등응답+AfterCommit | 5 | 4 | 5 | 3 | 균형 최적 |

선택 사유 요약:
- 정합성 5점을 확보하면서도, 사용자/운영 관점 실패 체감을 낮출 수 있는 유일한 조합

---

## 6. 권장 To-Be 설계 상세

## 6.1 요청 처리 시퀀스

1. `Idempotency-Key` 추출
2. 키 중복 조회
   - 이미 성공 응답 존재: 즉시 동일 응답 반환
3. Redis 락 획득 시도 (`video:complete:{videoId}`)
4. 트랜잭션 시작
5. Video 상태 확인/전이
6. Job 생성 시 DB 유니크 적용
7. 크레딧 차감 + 차감 중복 방지 키 기록
8. 멱등 응답 스냅샷 저장
9. 트랜잭션 커밋
10. 커밋 후 이벤트 발행(또는 outbox consume)
11. 락 해제

## 6.2 DB 제약 권장안

- `job(video_id)` 유니크 제약
- 크레딧 중복 방지용 키 도입
  - 예: `credit_history(deduction_key)` 유니크
  - `deduction_key = "VIDEO_PROCESSING:" + videoId`

## 6.3 멱등 응답 계약

- 동일 `Idempotency-Key` + 동일 사용자 + 동일 endpoint + 동일 payload hash
  - 기존 응답 그대로 재반환 (`200 OK`)
- 동일 키, payload 다름
  - `409 Conflict`

저장 항목:
- `idempotency_key`, `member_id`, `endpoint`, `request_hash`, `status_code`, `response_body`, `expires_at`

## 6.4 Redis 락 파라미터 가이드

- wait time: 짧게 (예: 200~500ms)
- lease/watchdog: 트랜잭션 + 외부 지연을 감안해 충분히
- 실패 시 동작:
  - 즉시 500 금지
  - 멱등 조회 경로로 우회 후 가능한 성공 응답 반환

---

## 7. 엣지 케이스와 방어 전략

## 7.1 사용자 더블 클릭 (수 ms 간격)

- 1차 요청 락 획득/처리
- 2차 요청은 락 대기 실패 또는 멱등 키 hit
- 결과: 동일 성공 응답 수렴

## 7.2 앱 타임아웃 후 재전송

- 서버에서 1차 요청은 성공했으나 클라이언트가 응답 못 받음
- 동일 키 재전송 시 저장된 응답 재반환
- 결과: 중복 작업 없음, 사용자 체감 정상

## 7.3 Redis 장애

- Redis 락 불가
- DB 유니크가 최종 방어 수행
- 결과: 성능은 저하될 수 있어도 정합성 유지

## 7.4 DB 커밋 직전 장애

- 커밋 실패 시 after-commit 발행 안 됨
- 결과: "메시지 먼저, DB 나중" 불일치 방지

## 7.5 동일 키 다른 payload 공격/오사용

- request hash 불일치 탐지
- `409 Conflict`
- 결과: 키 재사용 오염 차단

## 7.6 락 만료 후 중복 진입

- Redis 락 레이어가 놓치더라도 DB 유니크가 중복 insert 차단
- 결과: 최종 정합성 유지

## 7.7 중복 차감 우려

- 크레딧 차감에 비즈니스 키/유니크 부여
- 결과: Job과 별개로 금전성 데이터 안전 확보

---

## 8. 면접관/리뷰어 공격 질문 대비 Q&A

Q1. "DB 유니크만 있으면 되지, Redis 왜 필요하죠?"
- A: 유니크만으로 정합성은 보장되지만, 고경합 시 예외 폭증/재시도 폭증으로 운영비가 큽니다. Redis는 유니크 앞단에서 충돌량을 줄여 p95 지연과 에러율을 낮춥니다.

Q2. "Redis 락이 깨지면 끝 아닌가요?"
- A: 그래서 Redis를 최종 보장으로 쓰지 않습니다. 최종 보장은 DB 유니크입니다. Redis는 성능 계층입니다.

Q3. "비관적 락과 Redis 락 둘 다 중복 아닌가요?"
- A: 역할이 다릅니다. 비관적 락은 DB row 직렬화, Redis는 앱 레벨 진입 완화입니다. 다만 최종 설계에서는 DB 유니크를 중심으로 두고, 비관적 락은 축소/제거 검토가 가능합니다.

Q4. "멱등 응답이 왜 필요하죠? 그냥 중복이면 409 주면 되잖아요."
- A: 모바일/인터넷 환경에서 재시도는 정상 동작입니다. 409 남발은 사용자와 운영자 모두에게 노이즈입니다. 동일 요청은 동일 성공 응답으로 수렴시키는 게 API 품질이 높습니다.

Q5. "AFTER_COMMIT 안 하고 그냥 바로 SQS 보내면 뭐가 문제죠?"
- A: 트랜잭션 롤백 시 큐에는 작업이 남고 DB에는 상태가 없을 수 있습니다. 장애 복구 난이도가 급증합니다. 외부 발행은 커밋 후가 원칙입니다.

Q6. "Outbox까지 꼭 필요합니까?"
- A: 트래픽/장애 빈도가 낮으면 `publishEvent + AFTER_COMMIT`으로 시작 가능. 다만 발행 내구성/재처리 가시성이 필요하면 Outbox가 더 강합니다.

Q7. "Idempotency-Key TTL은 얼마가 적절합니까?"
- A: 클라이언트 재시도 윈도우를 커버해야 하므로 보통 수 시간~24시간. 비즈니스/트래픽/저장비용에 따라 조정하며 만료 정책을 명시해야 합니다.

Q8. "Exactly-once 보장인가요?"
- A: 분산 시스템에서 end-to-end exactly-once는 매우 비싸고 제한적입니다. 이 설계는 effectively-once(중복 무해화) 목표를 현실적으로 달성합니다.

Q9. "락 타임아웃/네트워크 분할에서 데이터 깨지지 않나요?"
- A: 깨지지 않게 하는 주체가 DB 유니크와 상태전이 제약입니다. 락은 최적화 레이어로 취급합니다.

Q10. "크레딧은 어떻게 회계적 정합성을 보장합니까?"
- A: 차감 이벤트에 유니크 비즈니스 키를 둬서 중복 차감 물리 차단, 실패 시 환불도 동일 키 기반으로 상쇄 추적합니다.

---

## 9. 구현 플랜 (강화 버전)

## Phase 1: 스키마 불변식 먼저 잠금

1. `job.video_id` 유니크 제약
2. `credit_history` 중복 차감 방지 키 추가
3. 기존 중복 데이터 정리 마이그레이션

완료 기준:
- 동시 요청에서도 중복 row 물리 삽입 불가

## Phase 2: 멱등 응답 계약 도입

1. `Idempotency-Key` 헤더 도입
2. idempotency 테이블 추가
3. request hash 검증 + 동일 응답 재반환

완료 기준:
- 네트워크 재시도에서 사용자 체감 에러 급감

## Phase 3: Redis 분산락 도입

1. Redisson 락 래퍼 구현
2. 락 실패 시 멱등 조회 fallback
3. 락 메트릭/알람 연결

완료 기준:
- 경합 구간 p95/p99 개선, unique violation 로그 감소

## Phase 4: 커밋 후 발행 정렬

1. `handleCreate` 직접 호출 제거
2. `ApplicationEventPublisher.publishEvent` 적용
3. 필요 시 Outbox 확장

완료 기준:
- 롤백-발행 불일치 재현 불가

## Phase 5: 검증/릴리스

1. 동시성/재시도/장애 주입 테스트
2. 카나리 배포 + 메트릭 검증
3. 점진 확대

---

## 10. 검증 시나리오 (테스트 관점)

필수 테스트:

- T1. 20~100 동시 요청, 동일 videoId, 동일 키
  - 기대: 1건 처리 + 나머지 동일 응답
- T2. 동일 videoId, 다른 키 난사
  - 기대: DB 유니크로 1건만 유효
- T3. Redis down
  - 기대: 처리량 저하 가능, 정합성 유지
- T4. 커밋 직전 예외 주입
  - 기대: 메시지 미발행
- T5. 동일 키 다른 payload
  - 기대: 409
- T6. 타임아웃 후 재요청
  - 기대: 최초 성공 응답 재반환

관측 지표:
- lock acquire success/fail
- idempotency hit ratio
- unique violation rate
- complete-upload p95/p99
- duplicate credit attempt count

---

## 11. 최종 판단

"왜 이 방법이어야 하는가"에 대한 최종 답변:

- 이 문제는 단순 동시성 문제가 아니라, "금전성 write + 비동기 발행 + 재시도 네트워크"가 합쳐진 복합 문제입니다.
- 따라서 단일 제어점(락 하나, 키 하나, 유니크 하나)으로는 반드시 빈틈이 생깁니다.
- `Redis + DB Unique + Idempotent Response + After-Commit Publish`는 각 계층의 책임을 분리하여,
  - 정합성,
  - 성능,
  - 운영성,
  - 사용자 경험
를 동시에 만족시키는 가장 실무적인 균형점입니다.

즉, 이 선택은 "기술 취향"이 아니라 "불변식 중심 설계"의 결과입니다.
