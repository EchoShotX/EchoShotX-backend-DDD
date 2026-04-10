# 면접 대비 플랜 — complete-upload 동시성/멱등성 구현

> 기준일: 2026-04-10  
> 대상: 토스페이먼츠 신입 면접

---

## 0. 포폴 기재 vs 실제 코드 — 불일치 정리

| 포폴 기재 | 실제 코드 | 판단 |
|---|---|---|
| "멱등키 → Redis에만 저장" | `VideoUploadIdempotencyService`가 **DB**(`video_upload_idempotency_record`)에 저장 | 포폴보다 강한 구현. Redis 장애에도 멱등성 유지됨 |
| "Redis 장애 시 fallback **미구현**" | `tryAcquireRedisLock` catch 블록 → `return false` → DB 비관락으로 자동 fallback | 이미 구현됨. 포폴 표현이 잘못됨 |
| "Outbox 폴러 재시도 시 중복 방지 모름" | SQS FIFO `messageDeduplicationId` **주석처리** 상태 — 실제 중복 방지 미완성 | 취약점 존재. 면접 공격 포인트 |
| 포폴 결과: "Redis 실패 상황 fallback 경로에서도 정합성 유지 검증" | Redis fallback 경로에 대한 **테스트 없음** | 테스트 미작성 |

---

## 1. 실제 취약점 분석

### 1-1. SQS messageDeduplicationId 미설정 (가장 위험)

**위치**: `JobPublisher.java:82-84`

```java
// FIFO면 groupId 필수
if (awsProps.getSqs().isFifo()) {
    builder.messageGroupId(awsProps.getSqs().getMessageGroupId());
    // 필요시 deduplicationId도 넣을 수 있음   ← 주석처리
    // builder.messageDeduplicationId(UUID.randomUUID().toString());
}
```

**문제**: Outbox 폴러가 SQS에 동일 jobId를 재전송할 때 (폴러 재시도 또는 다중 인스턴스 배포 시) SQS FIFO 큐의 내용 기반 중복제거(5분 window)에 의존하거나, UUID를 쓰면 오히려 매번 새 메시지가 들어감.

**올바른 구현**: `jobId`를 `messageDeduplicationId`로 사용
```java
builder.messageDeduplicationId(String.valueOf(message.getJobId()));
```

**왜**: 같은 jobId는 동일한 처리 단위이므로 SQS 레벨에서 5분 내 중복 발송 차단 가능.

---

### 1-2. Outbox Scheduler가 send()를 직접 호출 (재시도 책임 혼재)

**위치**: `JobOutboxPublisherScheduler.java:36`

```java
jobPublisher.send(message);  // sendWithRetry()가 아닌 send() 직접 호출
```

`JobPublisher`에는 `@Retryable`이 붙은 `sendWithRetry()`가 있지만, Scheduler는 `send()`를 직접 호출한다. 재시도는 Outbox 폴러의 `markRetry()`로만 처리되고 있음. 이 구조 자체는 의도적일 수 있으나, 면접에서 설명 가능해야 함.

**설명 포인트**: "Outbox 폴러 자체가 재시도 루프이므로, 내부에서 추가 @Retryable 재시도를 쌓으면 블로킹 시간이 늘어나 다른 이벤트 처리가 지연됩니다. 재시도 간격과 횟수는 Outbox의 exponential backoff(`markRetry`)로 통합 관리했습니다."

---

### 1-3. 멱등키 TTL 24시간의 근거 준비

**위치**: `VideoUploadIdempotencyService.java:32`

```java
private static final long DEFAULT_TTL_HOURS = 24L;
```

**답변 준비**: "업로드 완료 재시도는 보통 몇 분~몇 시간 이내입니다. 24시간은 네트워크 장애나 클라이언트 재시도 최대치를 커버하는 보수적 값입니다. 만료 후에는 DB 비관락 + 유니크 제약이 최종 방어선이므로 TTL이 지나도 정합성은 보장됩니다."

---

## 2. 구현 플랜 (면접 전 완료 권장)

### Task 1 — SQS FIFO deduplicationId 설정 [필수, 30분]

**파일**: `JobPublisher.java`

```java
// before
// builder.messageDeduplicationId(UUID.randomUUID().toString());

// after
builder.messageDeduplicationId(String.valueOf(message.getJobId()));
```

**면접 답변**: "Outbox 폴러가 재시도로 동일 job을 두 번 발송해도 SQS FIFO가 5분 window 내 중복을 차단합니다. jobId가 식별자이므로 jobId를 deduplicationId로 사용했습니다."

---

### Task 2 — Redis fallback 경로 통합 테스트 추가 [선택, 1시간]

`VideoUploadIdempotencyService`는 이미 DB 저장이므로, Redis가 죽어도 멱등성은 DB에서 보장됨. 이 사실을 테스트로 증명.

```java
@Test
void redis_장애_시에도_DB_멱등키로_중복_요청_차단() {
    // given: RedisLockService가 RuntimeException 던지도록 모킹
    when(redisLockService.tryLock(any(), any(), any())).thenThrow(new RuntimeException("Redis down"));
    
    // when: 동일 idempotencyKey로 2회 요청
    // then: 첫 번째만 처리, 두 번째는 캐시된 응답 반환
}
```

---

### Task 3 — 테스트 4.3건 측정 근거 문서화 [필수, 20분]

현재 테스트 코드에서 "평균 4.3건"의 수치가 어떤 설정에서 나왔는지 확인 후 정리.

확인 사항:
- 스레드 몇 개로 측정했는가?
- CountDownLatch로 동시 시작 보장했는가?
- 몇 회 반복 평균인가?

---

## 3. 면접 예상 질문 & 모범 답변

### Q1. "Redis 장애 시 멱등성 어떻게 보장하나요?"

**핵심**: 포폴에는 "Redis에만 저장"이라고 적혔지만 실제 구현은 다름. 이 불일치를 솔직하게 설명하되 실제 구현이 더 강하다는 것을 보여줌.

> "처음 설계에서는 Redis 캐시로 빠른 중복 체크를 고려했지만, Redis 단일 장애점 문제를 고려해 멱등 레코드를 DB에 저장하는 방식으로 구현했습니다. `video_upload_idempotency_record` 테이블에 `(member_id, video_id, idempotency_key)` 유니크 제약을 걸어, Redis가 완전히 다운되어도 DB 레코드 조회로 멱등성이 유지됩니다. Redis는 락(직렬화)에만 사용하고, 멱등 상태는 DB가 단독 보장합니다."

---

### Q2. "DB 락 fallback은 어떻게 구현했나요?"

> "`tryAcquireRedisLock()`에서 Redis 연결 자체가 실패하면 `RuntimeException`을 catch하고 `false`를 반환합니다. 이후 로직은 `processCompleteUpload()`로 이어지는데, 이 안에서 `videoAdaptor.queryByIdWithLock(videoId)`로 DB 비관락을 획득합니다. 즉, Redis 락 획득 실패 시 자동으로 DB 비관락만으로 직렬화하는 구조입니다. Redis가 없어도 DB 락 + 유니크 제약이 최종 안전망입니다."

---

### Q3. "비관락이랑 유니크키 중복 아닌가요? 왜 둘 다 쓰나요?"

> "역할이 다릅니다. 비관락은 Video 상태를 직렬로 검증해서 PENDING_UPLOAD가 아닌 경우 아예 처리를 막습니다. 유니크 제약은 코드 버그나 락이 누락된 예외 경로에서도 DB 레벨이 마지막으로 중복을 차단하는 최후 방어선입니다. 금전 도메인에서는 한 레이어 방어가 깨져도 다음 레이어가 막는 다층 구조가 맞다고 판단했습니다."

---

### Q4. "Outbox 폴러 재시도 시 중복은요?"

> "두 가지로 방어합니다. 첫째, `job_outbox_event`의 `status`가 `SENT`로 바뀌면 폴러가 더 이상 조회하지 않으므로 정상 경로에서는 중복 발송이 없습니다. 둘째, SQS FIFO 큐에 `messageDeduplicationId`를 `jobId`로 설정해서 폴러 재시작/다중 인스턴스 등 비정상 경로에서도 5분 window 내 중복 발송이 SQS 레벨에서 차단됩니다."

---

### Q5. "멱등키 만료 기간 24시간, 근거는?"

> "업로드 완료 재시도의 현실적 최대 시간을 기준으로 설정했습니다. 네트워크 장애 후 클라이언트가 최대 몇 시간 내 재시도한다고 가정하고 24시간을 보수적으로 잡았습니다. 만료 후에는 DB 비관락과 유니크 제약이 최종 방어이므로 TTL이 지나도 중복 Job 생성이나 중복 차감은 발생하지 않습니다."

---

### Q6. "테스트 4.3건, 스레드 몇 개로 측정했나요?"

> "20개 스레드를 `CountDownLatch`로 동시에 출발시켜 동일 videoId로 `complete-upload`를 호출했습니다. `@SpringBootTest` 통합 테스트로 실제 DB에서 race condition을 재현했고, 3회 반복 평균 Job 생성 수가 4.3건이었습니다. 비관락 + 유니크 제약 적용 후에는 1건으로 수렴했습니다."
>
> **코드 위치**: `CompleteVideoUploadUseCaseConcurrencyTest.java` — `CONCURRENT_THREADS = 20`, `TEST_ITERATIONS = 3`

---

### Q7. "단일 트랜잭션에 상태변경 + 크레딧 차감 + Job 생성을 묶은 이유는?" (예상 추가 공격)

> "원자성 보장입니다. 상태 변경 후 크레딧 차감이 실패하면 Video만 PROCESSING 상태가 되고 크레딧은 유지되는 불일치가 생깁니다. 하나의 트랜잭션으로 묶어 셋 중 하나라도 실패하면 전부 롤백되게 했습니다. SQS 발송은 Outbox로 분리해서 트랜잭션 바깥에서 별도 재시도하므로, DB 커밋 후 SQS 장애가 나도 재발송이 가능합니다."

---

### Q8. "FIFO 큐에서 모든 Job이 같은 messageGroupId를 쓰면 처리량 병목 아닌가요?" (심화)

> "맞습니다. 현재 단일 그룹으로 직렬화하면 처리량이 제한됩니다. videoId를 messageGroupId로 사용하면 같은 영상 요청만 직렬화되고 다른 영상은 병렬 처리 가능합니다. 현재는 단순화를 위해 단일 그룹을 사용했지만, 트래픽이 늘면 videoId 기반 샤딩으로 개선할 수 있습니다."

---

## 4. 실제 코드 흐름 한 줄 요약 (구두 연습용)

```
요청 수신
  → 멱등키 있으면 DB에서 기존 응답 조회 (있으면 즉시 반환)
  → Redis 락 시도 (실패하면 DB 락만으로 진행, fallback)
  → DB 비관락으로 Video 조회 → PENDING_UPLOAD 검증
  → 상태변경 + 크레딧 차감 + Job 생성 (단일 트랜잭션)
  → Outbox 이벤트 저장 (DB 커밋과 함께)
  → 응답 반환 + 멱등 레코드 DB 저장
  → 별도 스케줄러가 Outbox 폴링 → SQS 발송 (재시도 가능)
```

---

## 5. 포트폴리오 다듬기 플랜

### 원칙
- 구현 안 된 것은 쓰지 않는다
- 구현된 것인데 더 정확하게 쓸 수 있으면 고친다
- 숫자/구체적 수치가 있으면 반드시 넣는다 (면접관은 수치를 파고듦)

---

### 수정 1 — 해결 방안: 멱등키 저장 방식 명확화 [필수]

**현재 포폴**:
> Idempotency-Key + request hash를 도입해 같은 재시도 요청은 같은 응답 반환, 같은 키의 다른 payload는 충돌로 처리

**문제**: 저장 위치가 불명확해서 "Redis에 저장하나요?"라고 물으면 흔들림.

**수정 후**:
> Idempotency-Key + videoId + request payload의 SHA-256 hash를 `video_upload_idempotency_record` 테이블에 저장해 DB 레벨에서 멱등성을 보장한다. `(member_id, video_id, idempotency_key)` 유니크 제약으로 동시 저장 경합도 차단하며, Redis 장애와 무관하게 멱등성이 유지된다. 같은 키에 다른 payload가 오면 400 충돌 오류로 처리한다.

---

### 수정 2 — 해결 방안: Redis fallback 구현 내용 추가 [필수]

**현재 포폴**:
> (Redis fallback 언급 없음 / 구현 못했다고 메모만 있음)

**문제**: 실제로는 구현되어 있는데 포폴에 없음.

**추가할 내용**:
> Redis 락 획득 시 연결 장애가 발생하면 예외를 catch해 락 미획득으로 처리하고, 이후 로직은 DB 비관락만으로 직렬화한다. Redis는 선택적 성능 레이어이고, DB 비관락 + 유니크 제약이 정합성의 최종 보장자다.

---

### 수정 3 — 해결 방안: Outbox 재시도 수치 추가 [필수]

**현재 포폴**:
> 메시지 발행은 즉시 발행에서 Outbox로 전환하여 DB 커밋 후 재시도 가능한 구조로 변경

**문제**: "재시도 가능한 구조"라고만 쓰여 있어서 "구체적으로 어떻게 재시도하나요?"에 답하기 어려움.

**수정 후**:
> 메시지 발행을 Outbox 패턴으로 분리해 DB 커밋과 원자적으로 이벤트를 저장하고, 별도 스케줄러(500ms 주기)가 PENDING 이벤트를 폴링해 SQS로 발송한다. 발송 실패 시 exponential backoff(1s→2s→4s…최대 60s)로 최대 10회 재시도하며, 전부 실패 시 FAILED 상태로 기록해 수동 재처리 가능하다. SQS FIFO의 `messageDeduplicationId`를 jobId로 설정해 폴러 재시작/다중 인스턴스 상황에서도 중복 발송을 SQS 레벨에서 차단했다.

> ※ Task 1 구현 완료 후 마지막 문장 추가

---

### 수정 4 — 결과: 오해 소지 있는 문장 삭제 또는 수정 [필수]

**현재 포폴 결과 섹션**:
> Redis 실패 상황을 가정해 fallback 경로(DB 기반)에서도 정합성 유지 여부 검증

**문제**: "검증"이라고 써 있으나 실제 테스트가 없음. 면접관이 "테스트 코드 보여줄 수 있나요?"라고 물으면 근거 없음.

**선택지 A — 테스트 작성 후 유지** (Task 2 완료 시):
> Redis 장애 시뮬레이션(`RedisLockService` 모킹)에서 DB 비관락만으로 중복 요청이 차단되고 멱등 레코드가 정상 저장됨을 통합 테스트로 확인했다.

**선택지 B — 테스트 미작성 시 문장 교체**:
> Redis 락은 선택적 성능 레이어로 설계되어, 장애 시 자동으로 DB 비관락으로 fallback된다. 멱등 레코드는 Redis가 아닌 DB에 저장되므로 Redis 장애와 무관하게 멱등성이 구조적으로 보장된다.

---

### 수정 5 — 결과: "최대 5초 지연" 수치 수정 [필수]

**현재 포폴**:
> Outbox 폴러 실패 시 최대 5초 지연 후 재시도로 메시지 유실 없음

**문제**: 실제 코드는 exponential backoff이고 최대 60초까지 늘어남. 5초는 첫 번째 재시도 주기.

**수정 후**:
> Outbox 폴러 실패 시 exponential backoff(최대 60초)로 재시도하며, 최대 10회 재시도 후 FAILED 기록으로 메시지 유실을 방지한다.

---

### 추가 6 — 해결 방안: 설계 선택 근거에 "Redis+DB 조합"이 빠진 이유 보강 [선택]

현재 포폴의 설계 선택 근거에 "Redis 락만 사용 → Redis 장애 시 무결성 보장 불가"라고 되어 있는데, 최종 선택이 "DB 락 + Redis 락(선택적) 조합"임을 더 명확하게 연결하는 문장 추가 가능.

> Redis와 DB 락을 계층으로 분리해, Redis는 빠른 직렬화 레이어, DB 비관락은 정합성 보장 레이어로 역할을 나눴다. Redis가 없어도 DB 레이어만으로 정합성이 보장되며, Redis는 부하 감소 목적으로 선택적으로 작동한다.

---

### 포폴 수정 우선순위 요약

| 순서 | 수정 위치 | 중요도 | 비고 |
|---|---|---|---|
| 1 | 해결 방안 — 멱등키 저장 방식 명확화 | 필수 | 면접 Q1 직결 |
| 2 | 결과 — "5초 지연" → "최대 60초 exponential backoff" | 필수 | 수치 오류 |
| 3 | 결과 — "Redis fallback 검증" 문장 교체 | 필수 | 테스트 없으면 위험 |
| 4 | 해결 방안 — Redis fallback 구현 내용 추가 | 필수 | 구현됐는데 포폴에 없음 |
| 5 | 해결 방안 — Outbox 재시도 수치 추가 | Task 1 후 | deduplicationId 구현 후 |
| 6 | 설계 근거 — Redis+DB 조합 설명 보강 | 선택 | 시간 있으면 |

---

## 6. 체크리스트

### 구현
- [x] Task 1: `JobPublisher.java` — `messageDeduplicationId(jobId)` 구현
- [ ] Task 2: Redis fallback 통합 테스트 작성 (선택)
- [x] Task 3: 테스트 코드에서 4.3건 측정 근거 수치 확인 — 20스레드, CountDownLatch, 3회 반복 평균

### 포트폴리오 수정
- [x] 수정 1: 멱등키 저장 위치 DB로 명확화
- [x] 수정 2: Redis fallback 구현 내용 추가
- [x] 수정 3: Outbox 재시도 수치 추가 (Task 1 완료 후 반영)
- [x] 수정 4: "Redis fallback 검증" 문장 교체 (선택지 B 적용 — 구조적 보장으로 설명)
- [x] 수정 5: "최대 5초" → "최대 60초 exponential backoff"

### 면접 연습
- [ ] Q1~Q8 답변 소리내서 2회 이상 연습
- [ ] 코드 흐름 한 줄 요약 보지 않고 말하기
- [ ] "포폴에 Redis에만 저장이라고 했는데요?" 질문 대비 답변 연습
