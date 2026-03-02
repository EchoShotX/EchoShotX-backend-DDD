# EchoShotX 비디오 업로드 처리 아키텍처 개선 보고서
## Redis 분산락 + 멱등성 처리 도입 계획

---

## 1. 문제 상황 (Problem Statement)

### 1.1 현재 아키텍처 개요

사용자가 Presigned URL을 통해 S3에 영상 업로드를 완료하면, `CompleteVideoUploadUseCase`가 호출된다. 이 UseCase는 단일 `@Transactional` 트랜잭션 안에서 아래 작업을 **순차적으로 모두 처리**한다.

```
[클라이언트 요청]
       │
       ▼
@Transactional 트랜잭션 시작 (DB 커넥션 점유 시작)
       │
       ├─ 1. DB: Video 비관적 락(PESSIMISTIC_WRITE) 획득
       ├─ 2. DB: Video 상태 검증
       ├─ 3. DB: Video 업로드 완료 처리 (상태 변경)
       ├─ 4. DB: 크레딧 차감 (Credit 테이블 UPDATE)
       ├─ 5. DB: Job 생성 (Job 테이블 INSERT)
       ├─ 6. Network I/O: SQS 메시지 전송 ← ⚠️ 핵심 문제
       └─ 7. DB: Video 상태 QUEUED로 변경
       │
트랜잭션 커밋 → DB 커넥션 반환
```

**문제 핵심:** DB 커넥션을 점유한 채로 외부 네트워크 I/O(SQS)를 수행하고 있다.

---

### 1.2 면접관이 지적한 3가지 치명적 결함

#### 결함 1: DB 커넥션 풀 고갈로 인한 장애 전파

```
[현재 구조]
DB 커넥션 점유 시작
    ├── Video 락 (수 ms)
    ├── Credit 차감 (수 ms)
    ├── Job 생성 (수 ms)
    └── SQS 전송 대기... (최대 수 초, 재시도 포함 수십 초) ← 커넥션 계속 점유
DB 커넥션 반환
```

- SQS가 일시적으로 응답 지연(네트워크 레이턴시, AWS 장애)되면, DB 커넥션이 최대 **수십 초** 이상 반환되지 않는다.
- HikariCP 기본 커넥션 풀은 10개. 동시 요청이 10개만 쌓여도 **전체 서버가 응답 불가** 상태가 된다.
- SQS 장애 하나가 DB 장애로 전파되고, DB 장애가 전체 서비스 장애로 전파되는 **연쇄 장애(Cascade Failure)** 가 발생한다.

#### 결함 2: 크레딧 이중 차감 위험

```
[타임라인]
T1: 요청A → 락 획득 → 크레딧 차감 → SQS 전송 타임아웃
T2: 요청A → 재시도 → 락 획득 → 크레딧 차감 (이중 차감!) → SQS 전송 성공
```

- 락 획득은 성공하지만 SQS 전송에서 타임아웃이 발생하면, 트랜잭션 롤백 여부에 따라 크레딧이 차감된 상태로 남을 수 있다.
- 클라이언트가 실패로 인식하고 재시도하면, **크레딧이 두 번 차감**될 수 있다.
- 결제/크레딧 도메인에서 이중 차감은 가장 심각한 데이터 정합성 오류다.

#### 결함 3: 다중 테이블 비관적 락으로 인한 데드락 가능성

```
[데드락 시나리오]
요청A: Video(id=1) 락 → Credit(userId=10) 락 시도
요청B: Credit(userId=10) 락 → Video(id=1) 락 시도
→ 교착 상태 (Deadlock)
```

- Video, Credit, Job 등 여러 테이블을 순차적으로 락하는 구조는 요청 순서가 다른 경우 데드락을 유발한다.
- DB 레벨 비관적 락은 교착 상태 감지 후 강제 롤백이 발생하며, 사용자 요청이 무작위로 실패한다.

---

## 2. 원인 분석 (Root Cause Analysis)

| 구분 | 원인 |
|------|------|
| **설계적 원인** | 트랜잭션 경계가 지나치게 넓음. DB 작업과 네트워크 I/O가 동일 트랜잭션 안에 묶여 있음 |
| **동시성 전략** | 분산 환경을 고려하지 않은 단순 DB 비관적 락 사용 |
| **멱등성 부재** | 동일 요청의 재시도를 구분하는 식별자가 없어, 중복 처리 방지 불가 |
| **트랜잭션 설계** | 외부 시스템(SQS) 호출이 트랜잭션 원자성 범위에 포함되어, 트랜잭션 의미 자체가 깨짐 |

---

## 3. 개선 방안 (Solution Design)

### 3.1 핵심 전략

```
비관적 락(DB Lock) → Redis 분산락 (Network Layer)
이중 처리 문제     → Redis 멱등성 키 (Idempotency Key)
트랜잭션 경계      → DB 작업 완료 후 이벤트 발행 (@TransactionalEventListener AFTER_COMMIT)
```

### 3.2 전체 아키텍처 변경

```
[개선된 구조]

[클라이언트 요청 - Idempotency-Key 헤더 포함]
       │
       ▼
[Redis] 멱등성 키 확인
  ├─ 이미 처리됨 → 저장된 응답 즉시 반환 (200 OK)
  └─ 처음 요청 → 계속 진행
       │
       ▼
[Redis] 분산락 시도 (video:{videoId}:lock)
  ├─ 락 획득 실패 (다른 서버가 처리 중) → 409 Conflict 반환
  └─ 락 획득 성공 → 계속 진행
       │
       ▼
@Transactional 트랜잭션 시작 (DB 커넥션 점유 시작)
  ├─ 1. DB: Video 상태 검증 (락 없이 SELECT)
  ├─ 2. DB: Video 업로드 완료 처리 (상태 변경)
  ├─ 3. DB: 크레딧 차감
  ├─ 4. DB: Job 생성
  └─ 5. ApplicationEventPublisher.publishEvent(JobCreatedEvent)
       │
트랜잭션 커밋 → DB 커넥션 즉시 반환 ← ⭐ 핵심 개선
       │
       ▼ (트랜잭션 커밋 이후)
@TransactionalEventListener(AFTER_COMMIT)
  └─ 6. Network I/O: SQS 메시지 전송 (DB 커넥션과 무관)
       │
       ▼
[Redis] 멱등성 키에 처리 완료 + 응답 저장 (TTL: 24h)
       │
       ▼
[Redis] 분산락 해제
```

---

## 4. 상세 구현 계획 (Implementation Plan)

### 4.1 의존성 추가

```gradle
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.redisson:redisson-spring-boot-starter:3.27.0'
```

### 4.2 Redis 분산락 구현

#### `RedisDistributedLockService.java`

```java
package com.example.echoshotx.shared.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisDistributedLockService {

    private final RedissonClient redissonClient;

    private static final long WAIT_TIME_SECONDS = 0L;   // 대기하지 않고 즉시 실패
    private static final long LEASE_TIME_SECONDS = 10L; // 10초 후 자동 해제 (데드락 방지)

    /**
     * 분산락을 획득하고 작업을 실행한다.
     * 락 획득 실패 시 LockAcquisitionFailedException을 던진다.
     *
     * @param lockKey  락 키 (예: "video:123:upload-complete")
     * @param supplier 락 안에서 실행할 작업
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(WAIT_TIME_SECONDS, LEASE_TIME_SECONDS, TimeUnit.SECONDS);

            if (!acquired) {
                log.warn("Failed to acquire distributed lock. key={}", lockKey);
                throw new LockAcquisitionFailedException("이미 처리 중인 요청입니다. key=" + lockKey);
            }

            log.debug("Distributed lock acquired. key={}", lockKey);
            return supplier.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionFailedException("락 획득 중 인터럽트 발생", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Distributed lock released. key={}", lockKey);
            }
        }
    }

    public static String videoUploadLockKey(Long videoId) {
        return "video:" + videoId + ":upload-complete";
    }
}
```

#### `LockAcquisitionFailedException.java`

```java
package com.example.echoshotx.shared.lock;

public class LockAcquisitionFailedException extends RuntimeException {
    public LockAcquisitionFailedException(String message) {
        super(message);
    }
    public LockAcquisitionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

### 4.3 멱등성 처리 구현

#### `IdempotencyService.java`

```java
package com.example.echoshotx.shared.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final String PREFIX = "idempotency:";

    /**
     * 이미 처리된 요청인지 확인하고, 처리된 경우 저장된 응답을 반환한다.
     */
    public <T> Optional<T> findProcessedResponse(String idempotencyKey, Class<T> responseType) {
        String redisKey = PREFIX + idempotencyKey;
        String stored = redisTemplate.opsForValue().get(redisKey);

        if (stored == null) {
            return Optional.empty();
        }

        try {
            T response = objectMapper.readValue(stored, responseType);
            log.info("Idempotent response found. key={}", idempotencyKey);
            return Optional.of(response);
        } catch (Exception e) {
            log.warn("Failed to deserialize idempotent response. key={}", idempotencyKey, e);
            return Optional.empty();
        }
    }

    /**
     * 처리 완료된 응답을 저장한다.
     */
    public void saveResponse(String idempotencyKey, Object response) {
        String redisKey = PREFIX + idempotencyKey;
        try {
            String serialized = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(redisKey, serialized, IDEMPOTENCY_TTL);
            log.info("Idempotent response saved. key={}", idempotencyKey);
        } catch (Exception e) {
            // 멱등성 저장 실패는 치명적 오류가 아님. 로깅 후 계속 진행
            log.warn("Failed to save idempotent response. key={}", idempotencyKey, e);
        }
    }
}
```

---

### 4.4 개선된 UseCase

#### `CompleteVideoUploadUseCase.java` (개선 버전)

```java
package com.example.echoshotx.video.application.usecase;

import com.example.echoshotx.credit.application.service.CreditService;
import com.example.echoshotx.job.application.event.JobCreatedEvent;
import com.example.echoshotx.job.application.service.JobService;
import com.example.echoshotx.job.domain.entity.Job;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import com.example.echoshotx.shared.idempotency.IdempotencyService;
import com.example.echoshotx.shared.lock.RedisDistributedLockService;
import com.example.echoshotx.video.application.adaptor.VideoAdaptor;
import com.example.echoshotx.video.application.service.VideoService;
import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.domain.entity.VideoStatus;
import com.example.echoshotx.video.domain.exception.VideoErrorStatus;
import com.example.echoshotx.video.domain.vo.VideoMetadata;
import com.example.echoshotx.video.presentation.dto.request.CompleteUploadRequest;
import com.example.echoshotx.video.presentation.dto.response.CompleteUploadResponse;
import com.example.echoshotx.video.presentation.exception.VideoHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CompleteVideoUploadUseCase {

    private final VideoAdaptor videoAdaptor;
    private final VideoService videoService;
    private final CreditService creditService;
    private final JobService jobService;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisDistributedLockService lockService;
    private final IdempotencyService idempotencyService;

    /**
     * @param idempotencyKey 클라이언트가 헤더로 전달하는 멱등성 키 (예: UUID)
     */
    public CompleteUploadResponse execute(
            Long videoId,
            CompleteUploadRequest request,
            Member member,
            String idempotencyKey) {

        // 1. 멱등성 확인: 이미 처리된 요청이면 저장된 응답을 즉시 반환
        if (idempotencyKey != null) {
            var cached = idempotencyService.findProcessedResponse(
                idempotencyKey, CompleteUploadResponse.class
            );
            if (cached.isPresent()) {
                log.info("Returning idempotent response. videoId={}, key={}", videoId, idempotencyKey);
                return cached.get();
            }
        }

        // 2. Redis 분산락 획득 → 중복 요청 차단
        String lockKey = RedisDistributedLockService.videoUploadLockKey(videoId);
        CompleteUploadResponse response = lockService.executeWithLock(lockKey, () -> processUpload(request, videoId, member));

        // 3. 처리 완료 후 멱등성 응답 저장
        if (idempotencyKey != null) {
            idempotencyService.saveResponse(idempotencyKey, response);
        }

        return response;
    }

    /**
     * 실제 비즈니스 처리 로직.
     * 분산락 안에서 호출되며, 트랜잭션은 이 메서드에 국한된다.
     * SQS 전송은 트랜잭션 커밋 이후 이벤트로 처리된다.
     */
    @Transactional
    public CompleteUploadResponse processUpload(CompleteUploadRequest request, Long videoId, Member member) {
        // 1. 비디오 조회 및 권한 검증 (비관적 락 제거 → 분산락으로 대체)
        Video video = videoAdaptor.queryById(videoId); // 일반 SELECT
        video.validateMember(member);

        // 2. 상태 검증 (멱등성 키가 없는 직접 재처리 방지)
        if (video.getStatus() != VideoStatus.PENDING_UPLOAD) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_ALREADY_PROCESSED);
        }

        // 3. 업로드 완료 처리
        VideoMetadata metadata = createVideoMetadata(request);
        video = videoService.completeUpload(video, metadata);

        // 4. 크레딧 차감
        creditService.useCreditsForVideoProcessing(member, video, video.getProcessingType());

        // 5. Job 생성
        Job job = jobService.createJob(
            member, video.getId(),
            video.getOriginalFile().getS3Key(),
            video.getProcessingType()
        );

        // 6. 이벤트 발행 (SQS 전송은 AFTER_COMMIT 이후에 실행됨 → DB 커넥션 반환 후)
        eventPublisher.publishEvent(JobCreatedEvent.builder()
            .jobId(job.getId())
            .videoId(job.getVideoId())
            .processingType(job.getProcessingType())
            .memberId(member.getId())
            .s3Key(job.getS3Key())
            .build());

        // 7. 대기열 상태로 변경
        video = videoService.enqueueForProcessing(video, null); // sqsMessageId는 이벤트 핸들러에서 업데이트

        log.info("Video upload processing completed. videoId={}, jobId={}", videoId, job.getId());
        return CompleteUploadResponse.from(video);

        // 트랜잭션 커밋 → DB 커넥션 즉시 반환
        // 이후 JobEventHandler.handleCreate()가 AFTER_COMMIT 타이밍에 SQS 전송 수행
    }

    private VideoMetadata createVideoMetadata(CompleteUploadRequest request) {
        return VideoMetadata.builder()
            .durationSeconds(request.getDurationSeconds())
            .width(request.getWidth())
            .height(request.getHeight())
            .codec(request.getCodec())
            .bitrate(request.getBitrate())
            .frameRate(request.getFrameRate())
            .build();
    }
}
```

---

### 4.5 개선된 JobEventHandler

#### `JobEventHandler.java` (SQS 실패 시 Job 상태 업데이트 포함)

```java
package com.example.echoshotx.job.application.handler;

import com.example.echoshotx.job.application.event.JobCreatedEvent;
import com.example.echoshotx.job.application.service.JobService;
import com.example.echoshotx.job.infrastructure.dto.JobMessage;
import com.example.echoshotx.job.infrastructure.publisher.JobPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventHandler {

    private final JobPublisher jobPublisher;
    private final JobService jobService;

    /**
     * AFTER_COMMIT: DB 트랜잭션이 완전히 커밋된 이후에 실행.
     * 이 시점에는 DB 커넥션이 이미 반환된 상태이므로, SQS 전송이 느려도 커넥션 풀에 영향 없음.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreate(JobCreatedEvent event) {
        log.info("Publishing SQS message after commit. jobId={}, videoId={}", event.getJobId(), event.getVideoId());

        JobMessage message = JobMessage.builder()
            .jobId(event.getJobId())
            .videoId(event.getVideoId())
            .processingType(event.getProcessingType().name())
            .memberId(event.getMemberId())
            .s3Key(event.getS3Key())
            .build();

        try {
            jobPublisher.sendWithRetry(message);
            log.info("SQS message published successfully. jobId={}", event.getJobId());
        } catch (Exception e) {
            // SQS 전송 최종 실패 시 → recover()에서 Job 상태를 SEND_FAILED로 마킹
            // 별도 배치/운영팀 재처리 대상이 됨
            log.error("SQS message publish failed permanently. jobId={}", event.getJobId(), e);
        }
    }
}
```

---

### 4.6 컨트롤러: Idempotency-Key 헤더 수신

#### `VideoController.java` (멱등성 키 수신 부분)

```java
@PostMapping("/{videoId}/complete-upload")
public ResponseEntity<CompleteUploadResponse> completeUpload(
        @PathVariable Long videoId,
        @RequestBody CompleteUploadRequest request,
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

    Member member = memberService.findById(principal.getId());
    CompleteUploadResponse response = completeVideoUploadUseCase.execute(videoId, request, member, idempotencyKey);
    return ResponseEntity.ok(response);
}
```

---

### 4.7 Redis 설정

#### `RedisConfig.java`

```java
package com.example.echoshotx.shared.config.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + host + ":" + port)
            .setConnectionPoolSize(10)
            .setConnectionMinimumIdleSize(2);
        return Redisson.create(config);
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

---

## 5. 개선 전/후 비교 (Before vs After)

### 5.1 구조 비교

| 항목 | 개선 전 | 개선 후 |
|------|---------|---------|
| **동시성 제어** | DB 비관적 락 (`SELECT ... FOR UPDATE`) | Redis 분산락 (Redisson, tryLock 0초 대기) |
| **SQS 전송 타이밍** | 트랜잭션 안에서 동기 전송 | 트랜잭션 커밋 이후 (`AFTER_COMMIT`) 이벤트 기반 |
| **DB 커넥션 점유 시간** | DB 작업 + SQS 네트워크 대기 포함 | DB 작업만 (수 ms 수준) |
| **멱등성 처리** | 없음 (재시도 시 중복 처리 가능) | Redis 기반 멱등성 키 (24h TTL) |
| **크레딧 이중 차감** | 재시도 시 발생 가능 | 멱등성 키로 원천 차단 |
| **데드락 가능성** | 다중 테이블 락으로 높음 | 없음 (DB 락 미사용) |
| **분산 환경 지원** | 단일 서버 기준으로만 안전 | 다중 서버 환경에서도 안전 |

### 5.2 장애 시나리오 비교

| 시나리오 | 개선 전 결과 | 개선 후 결과 |
|----------|-------------|-------------|
| SQS 응답 3초 지연 | DB 커넥션 3초 점유 → 풀 고갈 위험 | 커넥션 이미 반환됨 → 영향 없음 |
| 클라이언트 중복 요청 | 크레딧 이중 차감 가능 | 멱등성 키로 캐시된 응답 반환 |
| 서버 2대 동시 요청 처리 | 두 서버 모두 처리 시작 가능 | Redis 분산락으로 하나만 처리 |
| SQS 전송 최종 실패 | Job 상태 불일치 가능 | `markSendFailed()` 호출 → 재처리 가능 상태로 마킹 |

---

## 6. 성과 (Expected Outcomes)

### 6.1 기술적 성과

**커넥션 풀 안정성**
- DB 커넥션 점유 시간: 트랜잭션 내 DB 작업 시간만 (기존 대비 90% 이상 단축 추정)
- SQS 장애가 DB 커넥션 풀에 미치는 영향: 0

**데이터 정합성**
- 크레딧 이중 차감: 멱등성 키로 원천 차단 (재시도 시 캐시된 응답 반환)
- 중복 Job 생성: Redis 분산락 + Video 상태 검증 이중 방어

**시스템 신뢰성**
- 데드락 가능성: DB 비관적 락 제거로 해소
- 분산 서버 환경 지원: Redis 분산락으로 다중 서버에서도 정확히 1회 처리 보장

### 6.2 아키텍처 개선

- **관심사 분리**: DB 트랜잭션 로직 ↔ 외부 시스템 연동 로직이 명확히 분리됨
- **확장성**: 향후 서버 증설 시에도 동시성 문제 없이 수평 확장 가능
- **운영성**: SQS 전송 실패 시 `SEND_FAILED` 상태로 명시적 마킹 → 재처리 파이프라인 구성 가능

---

## 7. 구현 우선순위 및 일정

| 단계 | 작업 | 우선순위 |
|------|------|---------|
| 1단계 | Redis 의존성 추가 및 `RedisConfig` 설정 | 즉시 |
| 2단계 | `RedisDistributedLockService` 구현 | 즉시 |
| 3단계 | `CompleteVideoUploadUseCase`에서 DB 비관적 락 제거 + 분산락 적용 | 즉시 |
| 4단계 | `IdempotencyService` 구현 | 1주 내 |
| 5단계 | 컨트롤러에 `Idempotency-Key` 헤더 수신 추가 | 1주 내 |
| 6단계 | 통합 테스트 (동시 요청 시나리오, SQS 장애 시나리오) | 2주 내 |

---

## 8. 예외 및 엣지 케이스 처리

### 8.1 Redis 자체가 장애인 경우

```java
// RedisDistributedLockService에서 Redis 연결 실패 시
// 옵션 A: DB 비관적 락으로 Fallback (안전 우선)
// 옵션 B: 예외를 던져 요청 거절 (정합성 우선) ← 권장
```

결제/크레딧 도메인 특성상 **옵션 B(요청 거절)** 를 권장한다. Redis 장애 시 5xx를 반환하고, 클라이언트가 재시도하도록 유도하는 것이 이중 과금보다 낫다.

### 8.2 Idempotency-Key 없이 요청하는 경우

- 헤더 없는 요청도 처리는 가능하나, 멱등성 보장이 안 됨을 클라이언트에 명시
- 선택적으로 `required = true`로 변경하여 강제화 가능

### 8.3 멱등성 키 재사용 공격 (보안)

- 멱등성 키에 `memberId`를 포함시켜 타인의 키를 재사용하지 못하도록 방어

```java
// 멱등성 키 네임스페이스 예시
String redisKey = "idempotency:" + member.getId() + ":" + idempotencyKey;
```

---

## 9. 결론

이번 개선은 단순한 기술 교체가 아니라, **분산 시스템에서의 올바른 트랜잭션 설계 원칙**을 적용하는 작업이다.

> "트랜잭션은 가능한 짧게. 외부 시스템 호출은 트랜잭션 밖에서."

Redis 분산락과 멱등성 처리를 도입함으로써, 커넥션 풀 고갈·크레딧 이중 차감·데드락이라는 세 가지 치명적 결함을 동시에 해소하고, 분산 서버 환경에서도 안정적으로 동작하는 구조를 갖추게 된다.