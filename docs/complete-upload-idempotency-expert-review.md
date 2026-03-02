# Complete Upload 중복 Job 방지 방식 전문가 리뷰

## TL;DR

- 현재 `PESSIMISTIC_WRITE` 기반 접근은 **단기적으로 유효한 안전장치**입니다. 실제로 동시성 테스트에서도 중복 Job 생성 억제 효과가 확인됩니다.
- 다만 이 방식만으로는 "진짜 멱등성"을 완성했다고 보기 어렵습니다. 이유는 **DB 유니크 보장 부재**, **외부 메시지 전송 타이밍**, **재시도/네트워크 중복 요청 시 API 계약 부재** 때문입니다.
- 권장 방향은 "락 중심"에서 "**멱등성 중심(상태 전이 + 유니크 키 + outbox/event after commit)**"으로 전환하는 것입니다.

---

## 1) 현행 구현 분석 (As-Is)

### 1.1 요청 처리 흐름

`CompleteVideoUploadUseCase.execute()`는 아래 순서로 동작합니다.

1. `videoAdaptor.queryByIdWithLock(videoId)`로 비디오 행 비관락 획득  
   - 참조: `src/main/java/com/example/echoshotx/video/application/usecase/CompleteVideoUploadUseCase.java:52`  
   - 참조: `src/main/java/com/example/echoshotx/video/infrastructure/persistence/VideoRepository.java:36`
2. 상태가 `PENDING_UPLOAD`인지 검사 후 업로드 완료 전환
3. 크레딧 차감
4. Job 생성 + SQS 전송 핸들러 호출
5. Video 상태를 `QUEUED`로 전환

### 1.2 지금 방식의 장점

- **동일 `videoId` 경쟁 요청 직렬화**: 더블 클릭/동시 재시도에서 1건만 통과시키는 데 효과적
- **구현 난이도 낮음**: 도메인 구조 큰 변경 없이 빠르게 적용 가능
- **멀티 인스턴스에서도 유효**: DB 락이므로 애플리케이션 인스턴스가 여러 대여도 동작

### 1.3 핵심 한계 및 리스크

1. **락은 멱등성 그 자체가 아님**  
   락은 "동시 실행 구간" 제어일 뿐, "재시도 요청을 같은 결과로 수렴"시키는 API 계약(멱등 키, 동일 응답 재사용)이 없습니다.

2. **Job 테이블에 중복 불변식(Unique) 부재**  
   `JobRepository`는 `findByVideoId`만 있고, DB 레벨에서 `video_id` 중복을 막는 제약이 없습니다.  
   - 참조: `src/main/java/com/example/echoshotx/job/infrastructure/persistence/JobRepository.java:9`

3. **외부 I/O가 트랜잭션 경계와 섞여 있음**  
   현재 `jobEventHandler.handleCreate(event)`를 직접 호출합니다.  
   - 참조: `src/main/java/com/example/echoshotx/video/application/usecase/CompleteVideoUploadUseCase.java:86`  
   이 메서드는 `@TransactionalEventListener(AFTER_COMMIT)`가 붙어 있지만, "이벤트 발행"이 아니라 "직접 메서드 호출"이므로 실질적으로 즉시 실행됩니다. 결과적으로:
   - DB 커밋 전 SQS 전송이 일어날 수 있음
   - 이후 트랜잭션 롤백 시 "DB는 실패했는데 큐에는 메시지 있음" 불일치 가능
   - 락 보유 시간이 외부 네트워크 지연에 의해 늘어날 수 있음

4. **중복 요청 시 응답이 실패 중심**  
   현재는 상태가 이미 바뀐 요청에 `VIDEO_ALREADY_PROCESSED`를 던집니다. 기술적으로 맞지만, 멱등 API 관점에서는 "이미 처리됨"을 성공(또는 동일 응답)으로 취급하는 전략이 더 운영 친화적입니다.

5. **크레딧 차감 멱등 키 부재**  
   크레딧 이력(`credit_history`)에도 "같은 video 처리에 대한 단일 차감"을 강제하는 제약/키가 보이지 않습니다. 현재는 락과 상태 전이에 의존합니다.

---

## 2) 질문에 대한 직접 답변: "비관적 락이 옳은가?"

### 결론

- **단기 대응책으로는 옳습니다.**
- **장기 정답으로는 불충분합니다.**

즉, 현재 선택은 "나쁜 선택"이 아니라 "1차 방어선"입니다. 다만 실무에서 장애/재시도/부분실패를 고려하면, 락 하나에 핵심 정합성을 맡기는 구조는 유지비가 커집니다.

---

## 3) 대안(멱등성 처리) 비교

## 3.1 상태 전이 + 조건부 업데이트 (권장)

아이디어: `PENDING_UPLOAD -> UPLOAD_COMPLETED` 전이를 **원자적 조건 업데이트**로 수행합니다.

- 예: `update video set status='UPLOAD_COMPLETED', ... where id=:id and status='PENDING_UPLOAD'`
- 영향 row 수가 `1`이면 최초 처리 성공, `0`이면 이미 처리됨(멱등 hit)

장점:
- 락 경합/대기 감소
- 멱등 판단이 명확

주의:
- 이후 Job 생성/크레딧까지 포함한 트랜잭션 설계 필요

## 3.2 DB 유니크 제약 기반 단일성 보장 (필수 권장)

아이디어: "한 비디오당 활성 Job 1개" 같은 비즈니스 불변식을 DB로 강제합니다.

- 예: `job(video_id)` 유니크 (혹은 상태 조건 포함한 부분 유니크 인덱스)

장점:
- 앱 버그/락 누락이 있어도 최종 중복 삽입 차단
- 운영 중 가장 신뢰도 높은 안전장치

주의:
- 재처리 정책(FAILED 후 재생성 허용) 반영한 인덱스 설계 필요

## 3.3 Idempotency-Key 테이블 (API 멱등 계약)

아이디어: 클라이언트가 `Idempotency-Key`를 보내고, 서버는 `(memberId, endpoint, key)`로 결과를 저장/재사용합니다.

장점:
- 네트워크 재시도/타임아웃 후 재호출에 동일 응답 보장
- UX/모바일 환경에서 매우 유리

주의:
- 저장 TTL/응답 스냅샷 관리 필요

## 3.4 Outbox 패턴 + 커밋 후 발행 (강력 권장)

아이디어: 트랜잭션 내에서 outbox 레코드 저장 후, 별도 퍼블리셔가 커밋된 이벤트만 SQS 발행.

장점:
- DB 상태와 메시지 발행 불일치 최소화
- 재시도/복구 표준화

주의:
- 운영 컴포넌트(폴러/리트라이) 추가 필요

## 3.5 Redis 분산락

장점:
- 특정 고경합 구간에서 빠른 제어 가능

한계:
- DB 정합성 보장을 대체하지 못함
- 락 유실/만료/펜싱 토큰 등 추가 복잡도

실무 판단: 이 케이스에서는 DB row 단위 정합성이 핵심이므로 우선순위 낮음.

---

## 4) 이 프로젝트에 맞는 권장 아키텍처 (현실적인 순서)

### Phase 1 (빠른 안정화)

1. `job.video_id` 단일성 제약 추가 (정책에 맞는 unique)
2. 중복 요청 시 `VIDEO_ALREADY_PROCESSED`를 무조건 에러로 끝내지 말고, 상태가 `UPLOAD_COMPLETED/QUEUED/PROCESSING`이면 기존 결과를 반환하는 멱등 응답 정책 추가
3. `CompleteVideoUploadUseCase`에서 `JobCreatedEvent` 직접 호출 제거, 실제 `ApplicationEventPublisher.publishEvent(...)`로 바꾸고 `AFTER_COMMIT`에서만 SQS 발행

### Phase 2 (정석 멱등성)

1. `Idempotency-Key` 도입 (`POST /videos/{videoId}/complete-upload`)
2. 키-응답 저장소(테이블/Redis) 도입
3. 재호출 시 동일 응답 반환 (HTTP 200/201 일관 정책)

### Phase 3 (내구성 강화)

1. Outbox 패턴 도입
2. 소비자(Worker) 쪽도 `jobId` 기준 멱등 처리
3. 관측성 추가: duplicate-hit, lock-wait, unique-violation, outbox-retry 메트릭

---

## 5) 실무 관점의 날카로운 포인트

1. **락은 비즈니스 규칙이 아니라 동시성 제어 도구**입니다. 비즈니스 불변식은 반드시 DB 제약으로 닫아야 합니다.
2. **"정상 실패"를 줄여야 운영이 편해집니다.** 더블 클릭/재시도는 사용자 잘못이 아니라 네트워크 현실입니다. 이 경우 멱등 성공 응답이 UX/재시도 정책에 더 적합합니다.
3. **외부 메시징은 커밋 이후**가 원칙입니다. 현재 구조는 롤백 불일치 가능성이 있어 장애 시 디버깅 비용이 큽니다.
4. **크레딧은 돈입니다.** 차감/환불은 반드시 "중복 불가 키"를 가져야 합니다. (예: `(member_id, video_id, transaction_type=USAGE)` 제약 또는 비즈니스 키)

---

## 6) 최종 판단

- 현재 비관적 락 적용은 "문제를 빨리 막은 좋은 1차 조치"입니다.
- 그러나 프로덕션 품질의 멱등성으로 가려면, 다음 3가지는 필수입니다.
  1. **DB 유니크 제약으로 중복 생성 원천 차단**
  2. **커밋 후 이벤트 발행(또는 Outbox)으로 상태/메시지 정합성 확보**
  3. **API Idempotency-Key로 재시도 요청 동일 결과 보장**

이 3가지를 적용하면, 더블 클릭뿐 아니라 재시도 폭주/부분 실패/네트워크 타임아웃까지 안정적으로 커버할 수 있습니다.

---

## 참고 코드 위치

- `src/main/java/com/example/echoshotx/video/application/usecase/CompleteVideoUploadUseCase.java`
- `src/main/java/com/example/echoshotx/video/infrastructure/persistence/VideoRepository.java`
- `src/main/java/com/example/echoshotx/job/infrastructure/persistence/JobRepository.java`
- `src/main/java/com/example/echoshotx/job/application/handler/JobEventHandler.java`
- `src/main/java/com/example/echoshotx/credit/application/service/CreditService.java`
- `src/test/java/com/example/echoshotx/video/application/usecase/CompleteVideoUploadUseCaseConcurrencyTest.java`
