# Complete Upload 멱등성 강화 실제 구현 플랜

## 1. 목적과 완료 기준

## 1.1 목적

`POST /videos/{videoId}/complete-upload`의 중복 호출 상황(더블 클릭, 재시도, 동시 요청)에서 다음을 보장한다.

- 동일 비디오에 대해 Job 1개만 생성
- 크레딧 중복 차감 방지
- 동일 요청 재전송 시 동일 응답 반환
- DB 상태와 메시지 발행의 순서 정합성 보장

## 1.2 Definition of Done

- DB 제약으로 중복 Job 삽입이 물리적으로 불가
- `Idempotency-Key` 재호출 시 동일 `200` 응답 재반환
- Redis 장애 시에도 정합성 훼손 없음(DB 제약으로 방어)
- 커밋 실패 시 SQS 메시지 발행 없음
- 동시성/재시도 테스트 통과 및 관측 지표 정상

---

## 2. 구현 전략 요약

최종 구조는 `Redis 분산락 + DB 유니크 + 멱등 응답 + 커밋 후 발행`이다.

- Redis 분산락: 경합 완화(성능/예외 폭증 완충)
- DB 유니크: 중복 생성 최종 차단(정합성 최후 보루)
- 멱등 응답: 재시도 요청 동일 결과 수렴(API 계약)
- 커밋 후 발행: 상태-메시지 불일치 방지

---

## 3. 작업 범위와 코드 영향도

## 3.1 주요 수정 대상

- `src/main/java/com/example/echoshotx/video/presentation/controller/VideoController.java`
- `src/main/java/com/example/echoshotx/video/application/usecase/CompleteVideoUploadUseCase.java`
- `src/main/java/com/example/echoshotx/job/domain/entity/Job.java`
- `src/main/java/com/example/echoshotx/job/infrastructure/persistence/JobRepository.java`
- `src/main/java/com/example/echoshotx/credit/application/service/CreditService.java`
- `src/main/java/com/example/echoshotx/job/application/handler/JobEventHandler.java`

신규 컴포넌트(예상):

- Idempotency 엔티티/리포지토리/서비스
- Redis Lock 유틸(예: `DistributedLockService`)
- DB 마이그레이션 파일(프로젝트 마이그레이션 체계에 맞춰 생성)
- 메트릭/로깅 공통 유틸

---

## 4. 단계별 구현 플랜

## Phase 0. 사전 정리 (0.5~1일)

목표:
- 현재 DB/배포/마이그레이션 체계를 확인하고 변경 전략 확정

작업:
1. 운영/스테이징 데이터에서 `job.video_id` 중복 현황 점검
2. `complete-upload` 호출량, 실패율, 재시도 패턴 확인
3. 마이그레이션 적용 방식 확정(Flyway/Liquibase/수동)

산출물:
- 데이터 정리 쿼리/백업 계획
- 단계별 배포 체크리스트 초안

---

## Phase 1. DB 불변식 잠금 (1~2일)

목표:
- 중복 삽입을 DB에서 원천 차단

작업:
1. `job` 테이블 유니크 제약 추가
   - 기본안: `UNIQUE(video_id)`
   - 재처리 정책이 필요하면 이후 확장(상태 포함 전략)
2. `credit_history` 중복 차감 방지 키 추가
   - 예: `deduction_key` 컬럼 + unique
   - 규칙: `VIDEO_PROCESSING:{videoId}`
3. 기존 중복 데이터 정리

코드 반영:
- `Job` 엔티티에 제약 반영
- CreditHistory 도메인에 deduction key 생성 규칙 반영

검증:
- 동시 insert 시 unique violation으로 중복 row 미생성 확인

---

## Phase 2. 멱등 응답 계약 도입 (2~3일)

목표:
- 동일 요청 재시도를 정상 응답으로 수렴

작업:
1. `Idempotency-Key` 헤더 수용
   - `VideoController.completeUpload`에서 헤더 입력
2. idempotency 저장소 구현
   - 필드: `key`, `memberId`, `endpoint`, `requestHash`, `statusCode`, `responseBody`, `expiresAt`
3. 처리 플로우 통합
   - 시작 시 key 조회 -> hit면 즉시 응답
   - miss면 처리 후 응답 스냅샷 저장
4. 키 재사용 오염 방지
   - 동일 key + 다른 request hash -> `409`

API 계약:
- 최초 처리 성공: `200`
- 동일 key 재호출: 최초와 동일 body + `200`

검증:
- timeout 후 재요청 시 동일 응답 반환
- 동일 key 다른 payload 시 409

---

## Phase 3. Redis 분산락 도입 (1~2일)

목표:
- 고경합에서 DB 충돌량/예외량 감소

작업:
1. Redisson 기반 락 서비스 구현
   - 락 키: `video:complete:{videoId}`
   - 짧은 wait time + watchdog
2. `CompleteVideoUploadUseCase`에서 핵심 구간 락 적용
3. 락 실패 fallback
   - 즉시 500 금지
   - idempotency 조회 경로 우선

주의:
- 락 해제는 소유권 확인
- 락은 최적화 계층, 정합성 보장은 DB

검증:
- 경합 테스트에서 lock fail 증가 시에도 정합성 문제 없음

---

## Phase 4. 커밋 후 발행 정렬 (1~2일)

목표:
- DB 상태와 메시지 발행의 순서 정합성 확보

작업:
1. `CompleteVideoUploadUseCase`에서 `handleCreate` 직접 호출 제거
2. `ApplicationEventPublisher.publishEvent(JobCreatedEvent)`로 전환
3. `@TransactionalEventListener(AFTER_COMMIT)`에서만 SQS 전송
4. (선택) Outbox로 확장 가능한 포인트 분리

검증:
- 트랜잭션 롤백 시 메시지 미발행 재현 확인

---

## Phase 5. 테스트 확장 및 운영 배포 (2~3일)

목표:
- 기능/성능/운영성 모두 검증 후 안전 배포

테스트 추가:
1. 동시성: 동일 `videoId` + 다중 스레드 + 동일/상이 key 조합
2. 재시도: 클라이언트 타임아웃 후 재전송
3. 장애: Redis down, SQS 지연/실패
4. 롤백: DB 예외 주입 후 메시지 상태 확인

관측 지표:
- `complete_upload.lock.acquire.success/fail`
- `complete_upload.idempotency.hit`
- `complete_upload.db.unique_violation`
- `complete_upload.duration.p95/p99`

배포 방식:
1. 카나리(소수 트래픽)
2. 지표 확인
3. 점진 확대

롤백 기준:
- p95 지연 급증
- 5xx 비율 상승
- 비정상 unique violation 폭증

---

## 5. 상세 설계 메모

## 5.1 멱등 키 TTL

- 권장: 24시간(초기)
- 이유: 모바일 재시도 창구를 넉넉히 커버
- 운영 중 저장 용량/히트율 보고 조정

## 5.2 상태 기반 응답 수렴

`video.status`가 아래면 멱등 성공으로 응답 가능:
- `UPLOAD_COMPLETED`, `QUEUED`, `PROCESSING`, `COMPLETED`

정책:
- 비즈니스 에러(권한 불일치, video 미존재)만 실패
- 중복 호출은 가능하면 성공으로 수렴

## 5.3 크레딧 안정성

- 차감과 Job 생성을 같은 트랜잭션으로 묶고
- 차감 이력에 중복 방지 키를 부여
- 실패 시 보상/환불 규칙을 같은 키 기반으로 추적

---

## 6. 리스크와 대응

리스크 1: 스키마 변경 중 잠금/다운타임
- 대응: 오프피크 적용, 사전 인덱스 생성 전략, 백업/롤백 쿼리

리스크 2: 멱등 저장소 누락으로 재시도 실패
- 대응: 처리 성공 직전 저장이 아닌, 트랜잭션 내 원자 저장

리스크 3: Redis 장애 시 처리 지연
- 대응: DB 중심 fallback, 락 실패 시 재조회 응답 정책

리스크 4: 기존 클라이언트 헤더 미전송
- 대응: 초기에는 서버 생성 key 허용, 이후 필수화 단계적 전환

---

## 7. 실행 순서(요약)

1. DB 유니크/중복 방지 키 적용
2. Idempotency-Key 계약 및 저장소 구현
3. Redis 락 추가
4. 커밋 후 발행 구조 정리
5. 테스트/메트릭/카나리 배포

핵심 원칙:
- 정합성(불변식) 먼저, 성능 최적화는 그 다음

---

## 8. 예상 일정

- 총 7~12영업일 (환경/리뷰/배포 승인 포함)
  - Phase 0: 0.5~1d
  - Phase 1: 1~2d
  - Phase 2: 2~3d
  - Phase 3: 1~2d
  - Phase 4: 1~2d
  - Phase 5: 2~3d

---

## 9. 최종 한 줄

이 플랜은 "중복 요청을 막는다"가 아니라, "중복 요청이 와도 시스템이 흔들리지 않고 동일 결과로 수렴한다"를 목표로 한 프로덕션 설계 플랜이다.
