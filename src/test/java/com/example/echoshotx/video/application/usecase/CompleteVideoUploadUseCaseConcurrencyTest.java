package com.example.echoshotx.video.application.usecase;

import com.example.echoshotx.credit.application.service.CreditService;
import com.example.echoshotx.job.application.handler.JobEventHandler;
import com.example.echoshotx.job.application.service.JobService;
import com.example.echoshotx.job.domain.entity.Job;
import com.example.echoshotx.job.infrastructure.persistence.JobRepository;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.member.domain.entity.Role;
import com.example.echoshotx.member.infrastructure.persistence.MemberRepository;
import com.example.echoshotx.video.application.adaptor.VideoAdaptor;
import com.example.echoshotx.video.application.service.VideoService;
import com.example.echoshotx.video.domain.entity.ProcessingType;
import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.infrastructure.persistence.VideoRepository;
import com.example.echoshotx.video.presentation.dto.request.CompleteUploadRequest;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CompleteVideoUploadUseCase 동시성 테스트.
 * 
 * <p>비관락 적용 전후를 비교하여 Job 중복 생성 방지 효과를 검증합니다.
 */
@Slf4j
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("CompleteVideoUploadUseCase 동시성 테스트")
class CompleteVideoUploadUseCaseConcurrencyTest {

    @Autowired
    private CompleteVideoUploadUseCase completeVideoUploadUseCase;

    @Autowired
    private VideoAdaptor videoAdaptor;

    @Autowired
    private VideoService videoService;

    @Autowired
    private CreditService creditService;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobEventHandler jobEventHandler;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private JobRepository jobRepository;

    private Member testMember;
    private CompleteUploadRequest testRequest;
    private CompleteVideoUploadUseCaseWithoutLock completeVideoUploadUseCaseWithoutLock;
    private static final int CONCURRENT_THREADS = 20;
    private static final int TEST_ITERATIONS = 3;

    @BeforeEach
    void setUp() {
        // 비관락 미적용 UseCase 인스턴스 생성
        completeVideoUploadUseCaseWithoutLock = new CompleteVideoUploadUseCaseWithoutLock(
                videoAdaptor,
                videoService,
                creditService,
                jobService,
                jobEventHandler
        );
        // 기존 데이터 정리
        jobRepository.deleteAll();
        videoRepository.deleteAll();
        memberRepository.deleteAll();

        // 테스트용 Member 생성
        testMember = Member.builder()
                .username("test-user-" + System.currentTimeMillis())
                .email("test@example.com")
                .role(Role.USER)
                .currentCredits(100000) // 충분한 크레딧 제공
                .build();
        testMember = memberRepository.save(testMember);

        // 테스트용 Request 생성
        testRequest = new CompleteUploadRequest(
                120.5, // durationSeconds
                1920,  // width
                1080,  // height
                "h264", // codec
                5_000_000L, // bitrate
                30.0 // frameRate
        );
    }

    @Test
    @DisplayName("비관락 적용 전: 동시 실행 시 Job 중복 생성 발생")
    void testWithoutLock_ConcurrentExecution_CreatesMultipleJobs() throws Exception {
        log.info("=== 비관락 적용 전 테스트 시작 ===");
        
        TestResult result = runConcurrentTest(
                completeVideoUploadUseCaseWithoutLock,
                "비관락 미적용"
        );

        log.info("비관락 미적용 결과: 생성된 Job 개수={}, 성공한 스레드={}, 실패한 스레드={}",
                result.getJobCount(), result.getSuccessCount(), result.getFailureCount());

        // 비관락 미적용 시 여러 Job이 생성될 수 있음
        assertThat(result.getJobCount())
                .as("비관락 미적용 시 여러 Job이 생성될 수 있음")
                .isGreaterThan(1);
    }

    @Test
    @DisplayName("비관락 적용 후: 동시 실행 시 Job 중복 생성 방지")
    void testWithLock_ConcurrentExecution_PreventsDuplicateJobs() throws Exception {
        log.info("=== 비관락 적용 후 테스트 시작 ===");
        
        TestResult result = runConcurrentTest(
                completeVideoUploadUseCase,
                "비관락 적용"
        );

        log.info("비관락 적용 결과: 생성된 Job 개수={}, 성공한 스레드={}, 실패한 스레드={}",
                result.getJobCount(), result.getSuccessCount(), result.getFailureCount());

        // 비관락 적용 시 1개의 Job만 생성되어야 함
        assertThat(result.getJobCount())
                .as("비관락 적용 시 1개의 Job만 생성되어야 함")
                .isEqualTo(1);
        
        // 대부분의 스레드는 실패해야 함 (첫 번째만 성공)
        assertThat(result.getFailureCount())
                .as("대부분의 스레드는 실패해야 함")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("비관락 적용 전후 비교: 중복 생성 방지 효과 검증")
    void testComparison_WithAndWithoutLock_ShowsImprovement() throws Exception {
        log.info("=== 비관락 적용 전후 비교 테스트 시작 ===");
        
        List<TestResult> withoutLockResults = new ArrayList<>();
        List<TestResult> withLockResults = new ArrayList<>();

        // 여러 번 반복하여 평균값 계산
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            log.info("반복 테스트 {} / {}", i + 1, TEST_ITERATIONS);
            
            // 비관락 미적용 테스트
            TestResult withoutLock = runConcurrentTest(
                    completeVideoUploadUseCaseWithoutLock,
                    "비관락 미적용 (반복 " + (i + 1) + ")"
            );
            withoutLockResults.add(withoutLock);
            
            // 잠시 대기 (DB 상태 초기화)
            Thread.sleep(100);
            
            // 비관락 적용 테스트
            TestResult withLock = runConcurrentTest(
                    completeVideoUploadUseCase,
                    "비관락 적용 (반복 " + (i + 1) + ")"
            );
            withLockResults.add(withLock);
            
            // 잠시 대기
            Thread.sleep(100);
        }

        // 평균값 계산
        double avgWithoutLock = withoutLockResults.stream()
                .mapToInt(TestResult::getJobCount)
                .average()
                .orElse(0.0);
        
        double avgWithLock = withLockResults.stream()
                .mapToInt(TestResult::getJobCount)
                .average()
                .orElse(0.0);

        double avgSuccessWithoutLock = withoutLockResults.stream()
                .mapToInt(TestResult::getSuccessCount)
                .average()
                .orElse(0.0);
        
        double avgSuccessWithLock = withLockResults.stream()
                .mapToInt(TestResult::getSuccessCount)
                .average()
                .orElse(0.0);

        log.info("=== 테스트 결과 비교 ===");
        log.info("비관락 미적용 - 평균 Job 개수: {}, 평균 성공 스레드: {}", avgWithoutLock, avgSuccessWithoutLock);
        log.info("비관락 적용 - 평균 Job 개수: {}, 평균 성공 스레드: {}", avgWithLock, avgSuccessWithLock);
        log.info("중복 생성 감소율: {}%", 
                ((avgWithoutLock - avgWithLock) / avgWithoutLock * 100));

        // 비관락 적용 시 Job 개수가 현저히 줄어들어야 함
        assertThat(avgWithLock)
                .as("비관락 적용 시 평균 Job 개수는 1에 가까워야 함")
                .isLessThanOrEqualTo(2.0); // 허용 오차 고려
        
        assertThat(avgWithoutLock)
                .as("비관락 미적용 시 평균 Job 개수는 1보다 커야 함")
                .isGreaterThan(1.0);
    }

    /**
     * 동시성 테스트 실행
     */
    private TestResult runConcurrentTest(
            Object useCase,
            String testName) throws Exception {
        
        // 테스트용 Video 생성
        Video testVideo = Video.createForPresignedUpload(
                testMember.getId(),
                "test-video.mp4",
                10_000_000L,
                ProcessingType.AI_UPSCALING,
                "videos/test/test-video.mp4",
                "upload-id-" + System.currentTimeMillis(),
                LocalDateTime.now().plusHours(1)
        );
        testVideo = videoRepository.save(testVideo);
        Long videoId = testVideo.getId();

        // 기존 Job 정리
        jobRepository.deleteAll();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(CONCURRENT_THREADS);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        // 모든 스레드가 동시에 시작하도록 설정
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i;
            Future<?> future = executor.submit(() -> {
                try {
                    // 모든 스레드가 동시에 시작하도록 대기
                    startLatch.await();
                    
                    log.debug("[{}] {} - 스레드 {} 시작", testName, threadId, Thread.currentThread().getName());
                    
                    // Member를 다시 조회하여 최신 상태 유지
                    Member currentMember = memberRepository.findById(testMember.getId())
                            .orElseThrow();
                    
                    if (useCase instanceof CompleteVideoUploadUseCase) {
                        completeVideoUploadUseCase.execute(videoId, testRequest, currentMember);
                    } else if (useCase instanceof CompleteVideoUploadUseCaseWithoutLock) {
                        completeVideoUploadUseCaseWithoutLock.execute(videoId, testRequest, currentMember);
                    }
                    
                    successCount.incrementAndGet();
                    log.debug("[{}] {} - 스레드 {} 성공", testName, threadId, Thread.currentThread().getName());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.debug("[{}] {} - 스레드 {} 실패: {}", testName, threadId, 
                            Thread.currentThread().getName(), e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
            futures.add(future);
        }

        // 모든 스레드 동시 시작
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        
        // 모든 스레드 완료 대기
        endLatch.await();
        long endTime = System.currentTimeMillis();

        executor.shutdown();

        // 생성된 Job 개수 확인
        List<Job> createdJobs = jobRepository.findByVideoId(videoId);
        int jobCount = createdJobs.size();

        log.info("[{}] 실행 시간: {}ms, 생성된 Job: {}개, 성공: {}개, 실패: {}개",
                testName, (endTime - startTime), jobCount, 
                successCount.get(), failureCount.get());

        return new TestResult(jobCount, successCount.get(), failureCount.get(), 
                endTime - startTime);
    }

    /**
     * 테스트 결과를 담는 내부 클래스
     */
    private static class TestResult {
        private final int jobCount;
        private final int successCount;
        private final int failureCount;
        private final long executionTimeMs;

        public TestResult(int jobCount, int successCount, int failureCount, long executionTimeMs) {
            this.jobCount = jobCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.executionTimeMs = executionTimeMs;
        }

        public int getJobCount() {
            return jobCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }
    }
}

