package com.example.echoshotx.video.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import com.example.echoshotx.credit.application.service.CreditService;
import com.example.echoshotx.credit.domain.entity.CreditHistory;
import com.example.echoshotx.credit.domain.entity.TransactionType;
import com.example.echoshotx.job.application.handler.JobEventHandler;
import com.example.echoshotx.job.application.service.JobService;
import com.example.echoshotx.job.domain.entity.Job;
import com.example.echoshotx.job.domain.entity.JobStatus;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.member.domain.entity.Role;
import com.example.echoshotx.video.application.adaptor.VideoAdaptor;
import com.example.echoshotx.video.application.service.VideoService;
import com.example.echoshotx.video.domain.entity.ProcessingType;
import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.domain.entity.VideoStatus;
import com.example.echoshotx.video.domain.vo.VideoFile;
import com.example.echoshotx.video.domain.vo.VideoMetadata;
import com.example.echoshotx.video.presentation.dto.request.CompleteUploadRequest;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CompleteVideoUploadUseCase 동시성 테스트.
 *
 * <p>테스트 목적:
 * <ul>
 *   <li>사용자가 빠르게 연속으로 요청할 때 Job 중복 생성 여부 확인</li>
 *   <li>SQS 메시지 중복 전송 여부 확인</li>
 *   <li>크레딧 중복 차감 여부 확인</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompleteVideoUploadUseCase 동시성 테스트")
class CompleteVideoUploadUseCaseConcurrencyTest {

    @Mock
    private VideoAdaptor videoAdaptor;

    @Mock
    private VideoService videoService;

    @Mock
    private CreditService creditService;

    @Mock
    private JobService jobService;

    @Mock
    private JobEventHandler jobEventHandler;

    @InjectMocks
    private CompleteVideoUploadUseCase completeVideoUploadUseCase;

    private Member testMember;
    private Video testVideo;
    private Video uploadCompletedVideo;
    private Video queuedVideo;
    private CompleteUploadRequest testRequest;
    private CreditHistory testCreditHistory;

    @BeforeEach
    void setUp() {
        // Given: 테스트 데이터 준비
        testMember =
                Member.builder()
                        .id(1L)
                        .username("testuser@example.com")
                        .email("testuser@example.com")
                        .role(Role.USER)
                        .currentCredits(1000)
                        .build();

        testRequest =
                new CompleteUploadRequest(
                        120.5, // durationSeconds
                        1920,  // width
                        1080,  // height
                        "h264", // codec
                        5_000_000L, // bitrate
                        30.0 // frameRate
                );

        // 초기 PENDING_UPLOAD 상태의 비디오
        testVideo =
                Video.builder()
                        .id(100L)
                        .memberId(1L)
                        .originalFile(
                                VideoFile.builder()
                                        .fileName("test-video.mp4")
                                        .fileSizeBytes(10_000_000L)
                                        .s3Key("videos/1/original/upload-id/20250101120000_test-video.mp4")
                                        .build())
                        .status(VideoStatus.PENDING_UPLOAD)
                        .processingType(ProcessingType.AI_UPSCALING)
                        .uploadId("upload-id-123")
                        .retryCount(0)
                        .build();

        // 업로드 완료된 비디오 (UPLOAD_COMPLETED 상태)
        uploadCompletedVideo =
                Video.builder()
                        .id(100L)
                        .memberId(1L)
                        .originalFile(testVideo.getOriginalFile())
                        .status(VideoStatus.UPLOAD_COMPLETED)
                        .processingType(ProcessingType.AI_UPSCALING)
                        .uploadId("upload-id-123")
                        .originalMetadata(
                                VideoMetadata.builder()
                                        .durationSeconds(120.5)
                                        .width(1920)
                                        .height(1080)
                                        .codec("h264")
                                        .bitrate(5_000_000L)
                                        .frameRate(30.0)
                                        .build())
                        .retryCount(0)
                        .build();

        // 큐에 등록된 비디오 (QUEUED 상태)
        queuedVideo =
                Video.builder()
                        .id(100L)
                        .memberId(1L)
                        .originalFile(testVideo.getOriginalFile())
                        .status(VideoStatus.QUEUED)
                        .processingType(ProcessingType.AI_UPSCALING)
                        .uploadId("upload-id-123")
                        .sqsMessageId("sqs-msg-uuid-12345")
                        .originalMetadata(uploadCompletedVideo.getOriginalMetadata())
                        .retryCount(0)
                        .build();

        // 크레딧 사용 내역
        testCreditHistory =
                CreditHistory.builder()
                        .id(1L)
                        .memberId(1L)
                        .videoId(100L)
                        .transactionType(TransactionType.USAGE)
                        .amount(-100)
                        .description("Video processing for BASIC_ENHANCEMENT")
                        .build();
    }

    @Test
    @DisplayName("동시 요청 시 Job 중복 생성 및 SQS 메시지 중복 전송 발생 확인")
    void execute_ConcurrentRequests_CreatesDuplicateJobsAndSendsDuplicateMessages()
            throws InterruptedException {
        // Given: 사용자가 빠르게 연속으로 API를 호출하는 상황 시뮬레이션
        // 실제로는 각 요청마다 UseCase.execute()가 호출됨
        int concurrentRequests = 5; // 동시 요청 수
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1); // 모든 스레드가 동시에 시작하도록
        CountDownLatch completeLatch = new CountDownLatch(concurrentRequests); // 모든 요청 완료 대기

        // Mock 설정: 모든 요청이 PENDING_UPLOAD 상태를 읽을 수 있도록 설정
        // (트랜잭션 경쟁 조건 시뮬레이션 - 모든 요청이 같은 상태를 읽음)
        given(videoAdaptor.queryById(100L)).willReturn(testVideo);
        given(videoService.completeUpload(eq(testVideo), any(VideoMetadata.class)))
                .willReturn(uploadCompletedVideo);
        given(creditService.useCreditsForVideoProcessing(any(), any(), any()))
                .willReturn(testCreditHistory);
        given(videoService.enqueueForProcessing(eq(uploadCompletedVideo), any(String.class)))
                .willReturn(queuedVideo);

        // Job 생성 시마다 새로운 Job 반환 (중복 생성 시뮬레이션)
        AtomicInteger jobIdCounter = new AtomicInteger(1);
        given(jobService.createJob(any(), eq(100L), any(), any()))
                .willAnswer(
                        invocation -> {
                            // 각 요청마다 새로운 Job 생성 (중복 생성 시뮬레이션)
                            int jobId = jobIdCounter.getAndIncrement();
                            return Job.builder()
                                    .id((long) jobId)
                                    .videoId(100L)
                                    .memberId(1L)
                                    .s3Key("videos/1/original/upload-id/20250101120000_test-video.mp4")
                                    .processingType(ProcessingType.AI_UPSCALING)
                                    .status(JobStatus.REQUESTED)
                                    .build();
                        });

        // When: 동시에 여러 요청 실행 (사용자가 빠르게 연속으로 클릭)
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            executorService.submit(
                    () -> {
                        try {
                            // 모든 스레드가 동시에 시작하도록 대기
                            startLatch.await();

                            // UseCase 실행 (API 호출 시뮬레이션)
                            completeVideoUploadUseCase.execute(100L, testRequest, testMember);

                        } catch (Exception e) {
                            // 예외 발생 시 로그 출력
                            System.err.println(
                                    "Request " + requestIndex + " failed: " + e.getMessage());
                            e.printStackTrace();
                        } finally {
                            completeLatch.countDown();
                        }
                    });
        }

        // 모든 스레드가 준비될 때까지 대기 후 동시에 시작
        Thread.sleep(100); // 스레드 준비 시간
        startLatch.countDown(); // 모든 스레드 동시 시작

        // 모든 요청 완료 대기 (최대 10초)
        boolean completed = completeLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: 중복 생성 확인
        assertThat(completed)
                .as("모든 요청이 완료되어야 함")
                .isTrue();

        // JobService.createJob이 여러 번 호출되었는지 확인
        // 동시 요청으로 인해 같은 videoId에 대해 여러 Job이 생성됨
        verify(jobService, atLeast(concurrentRequests))
                .createJob(any(), eq(100L), any(), any());

        // JobEventHandler.handleCreate가 여러 번 호출되었는지 확인 (SQS 전송 트리거)
        ArgumentCaptor<com.example.echoshotx.job.application.event.JobCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(
                        com.example.echoshotx.job.application.event.JobCreatedEvent.class);
        verify(jobEventHandler, atLeast(concurrentRequests)).handleCreate(eventCaptor.capture());

        List<com.example.echoshotx.job.application.event.JobCreatedEvent> capturedEvents =
                eventCaptor.getAllValues();

        // 각 이벤트의 jobId가 다른지 확인 (중복 Job 생성 확인)
        List<Long> jobIds =
                capturedEvents.stream()
                        .map(com.example.echoshotx.job.application.event.JobCreatedEvent::getJobId)
                        .toList();

        System.out.println("========================================");
        System.out.println("동시성 테스트 결과:");
        System.out.println("동시 요청 수: " + concurrentRequests);
        System.out.println("Job 생성 호출 횟수: " + capturedEvents.size());
        System.out.println("생성된 Job ID: " + jobIds);
        System.out.println("========================================");

        // 검증: 같은 videoId에 대해 여러 Job이 생성되었는지 확인
        assertThat(capturedEvents)
                .as("동시 요청으로 인해 Job이 %d번 이상 생성되어야 함 (현재: %d번)", concurrentRequests, capturedEvents.size())
                .hasSizeGreaterThanOrEqualTo(concurrentRequests);

        // 모든 Job이 같은 videoId를 가지고 있는지 확인
        assertThat(capturedEvents)
                .as("모든 Job이 같은 videoId를 가져야 함")
                .allMatch(event -> event.getVideoId().equals(100L));

        if (jobIds.size() > 1) {
            System.out.println(
                    "⚠️ 경고: 같은 videoId("
                            + 100L
                            + ")에 대해 "
                            + jobIds.size()
                            + "개의 Job이 생성되었습니다. (중복 생성 문제 발생)");
        }
    }

    @Test
    @DisplayName("동시 요청 시 크레딧 중복 차감 발생 확인")
    void execute_ConcurrentRequests_DeductsCreditsMultipleTimes()
            throws InterruptedException {
        // Given
        int concurrentRequests = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(concurrentRequests);

        given(videoAdaptor.queryById(100L)).willReturn(testVideo);
        given(videoService.completeUpload(eq(testVideo), any(VideoMetadata.class)))
                .willReturn(uploadCompletedVideo);
        given(creditService.useCreditsForVideoProcessing(any(), any(), any()))
                .willReturn(testCreditHistory);
        given(videoService.enqueueForProcessing(any(), any(String.class)))
                .willReturn(queuedVideo);

        AtomicInteger jobIdCounter = new AtomicInteger(1);
        given(jobService.createJob(any(), eq(100L), any(), any()))
                .willAnswer(
                        invocation -> {
                            int jobId = jobIdCounter.getAndIncrement();
                            return Job.builder()
                                    .id((long) jobId)
                                    .videoId(100L)
                                    .memberId(1L)
                                    .s3Key("videos/1/original/upload-id/20250101120000_test-video.mp4")
                                    .processingType(ProcessingType.AI_UPSCALING)
                                    .status(JobStatus.REQUESTED)
                                    .build();
                        });

        AtomicInteger creditDeductionCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < concurrentRequests; i++) {
            executorService.submit(
                    () -> {
                        try {
                            startLatch.await();
                            completeVideoUploadUseCase.execute(100L, testRequest, testMember);
                            creditDeductionCount.incrementAndGet();
                        } catch (Exception e) {
                            System.err.println("Request failed: " + e.getMessage());
                        } finally {
                            completeLatch.countDown();
                        }
                    });
        }

        Thread.sleep(100);
        startLatch.countDown();
        boolean completed = completeLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then
        assertThat(completed).isTrue();

        // 크레딧 차감이 여러 번 호출되었는지 확인
        int actualCreditDeductions = creditDeductionCount.get();
        assertThat(actualCreditDeductions)
                .as("크레딧이 %d번 차감되어야 함 (현재: %d번)", concurrentRequests, actualCreditDeductions)
                .isEqualTo(concurrentRequests);

        verify(creditService, atLeast(concurrentRequests))
                .useCreditsForVideoProcessing(any(), any(), any());

        System.out.println("========================================");
        System.out.println("크레딧 중복 차감 테스트 결과:");
        System.out.println("동시 요청 수: " + concurrentRequests);
        System.out.println("크레딧 차감 횟수: " + actualCreditDeductions);
        System.out.println("========================================");
    }

    @Test
    @DisplayName("동시 요청 시 SQS 메시지 전송 횟수 확인")
    void execute_ConcurrentRequests_SendsMultipleSqsMessages()
            throws InterruptedException {
        // Given: JobPublisher의 send 메서드 호출 횟수를 추적하기 위해
        // 실제로는 JobEventHandler를 통해 JobPublisher가 호출되므로
        // JobEventHandler.handleCreate 호출 횟수로 확인
        int concurrentRequests = 4;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(concurrentRequests);

        given(videoAdaptor.queryById(100L)).willReturn(testVideo);
        given(videoService.completeUpload(eq(testVideo), any(VideoMetadata.class)))
                .willReturn(uploadCompletedVideo);
        given(creditService.useCreditsForVideoProcessing(any(), any(), any()))
                .willReturn(testCreditHistory);
        given(videoService.enqueueForProcessing(any(), any(String.class)))
                .willReturn(queuedVideo);

        AtomicInteger jobIdCounter = new AtomicInteger(1);
        given(jobService.createJob(any(), eq(100L), any(), any()))
                .willAnswer(
                        invocation -> {
                            int jobId = jobIdCounter.getAndIncrement();
                            return Job.builder()
                                    .id((long) jobId)
                                    .videoId(100L)
                                    .memberId(1L)
                                    .s3Key("videos/1/original/upload-id/20250101120000_test-video.mp4")
                                    .processingType(ProcessingType.AI_UPSCALING)
                                    .status(JobStatus.REQUESTED)
                                    .build();
                        });

        // When
        for (int i = 0; i < concurrentRequests; i++) {
            executorService.submit(
                    () -> {
                        try {
                            startLatch.await();
                            completeVideoUploadUseCase.execute(100L, testRequest, testMember);
                        } catch (Exception e) {
                            System.err.println("Request failed: " + e.getMessage());
                        } finally {
                            completeLatch.countDown();
                        }
                    });
        }

        Thread.sleep(100);
        startLatch.countDown();
        boolean completed = completeLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then
        assertThat(completed).isTrue();

        // JobEventHandler.handleCreate가 여러 번 호출되었는지 확인
        // 이는 SQS 메시지 전송을 트리거하므로 중복 전송을 의미
        ArgumentCaptor<com.example.echoshotx.job.application.event.JobCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(
                        com.example.echoshotx.job.application.event.JobCreatedEvent.class);

        verify(jobEventHandler, atLeast(concurrentRequests)).handleCreate(eventCaptor.capture());

        List<com.example.echoshotx.job.application.event.JobCreatedEvent> capturedEvents =
                eventCaptor.getAllValues();

        assertThat(capturedEvents)
                .as("SQS 메시지 전송 이벤트가 %d번 발생해야 함", concurrentRequests)
                .hasSizeGreaterThanOrEqualTo(concurrentRequests);

        // 각 이벤트의 jobId가 다른지 확인 (중복 Job 생성 확인)
        List<Long> jobIds =
                capturedEvents.stream()
                        .map(com.example.echoshotx.job.application.event.JobCreatedEvent::getJobId)
                        .toList();

        System.out.println("========================================");
        System.out.println("SQS 메시지 중복 전송 테스트 결과:");
        System.out.println("동시 요청 수: " + concurrentRequests);
        System.out.println("SQS 전송 이벤트 수: " + capturedEvents.size());
        System.out.println("생성된 Job ID: " + jobIds);
        System.out.println("========================================");
    }
}
