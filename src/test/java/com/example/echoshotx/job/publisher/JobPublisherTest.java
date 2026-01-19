package com.example.echoshotx.job.publisher;

import com.example.echoshotx.job.application.service.JobService;
import com.example.echoshotx.job.infrastructure.dto.JobMessage;
import com.example.echoshotx.job.infrastructure.publisher.JobPublisher;
import com.example.echoshotx.shared.config.aws.props.AwsProps;
import com.example.echoshotx.video.domain.entity.ProcessingType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {
        JobPublisherTest.RetryTestConfig.class,
        JobPublisher.class
})
public class JobPublisherTest {

    @Configuration
    @EnableRetry
    static class RetryTestConfig {
        @Bean
        public AwsProps awsProps() {
            AwsProps props = new AwsProps();
            AwsProps.Sqs sqsProps = new AwsProps.Sqs();
            sqsProps.setQueueUrl("https://sqs.ap-northeast-2.amazonaws.com/712155057827/echoshotx-dev");
            sqsProps.setFifo(false);
            props.setSqs(sqsProps);
            return props;
        }
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
    @Autowired
    private JobPublisher jobPublisher;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private SqsClient sqsClient;

    private JobMessage jobMessage;

    @BeforeEach
    void setUp() {
        jobMessage = JobMessage.builder()
                .jobId(1L)
                .videoId(10L)
                .memberId(100L)
                .processingType(ProcessingType.AI_UPSCALING.name())
                .s3Key("test.mp4")
                .build();
    }

    @Test
    @DisplayName("첫 시도에 성공하면 재시도하지 않고 실패로 표시하지 않는다.")
    void sendWithRetry_successOnFirstTry_doesNotRetryOrMarkFailed() {
        SendMessageResponse response = SendMessageResponse.builder()
                .messageId("msg-123")
                .build();

        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(response);

        assertDoesNotThrow(() -> jobPublisher.sendWithRetry(jobMessage));

        // 1번만 호출됨 (재시도 없음)
        verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
        // 실패 mark 로직은 호출되지 않아야 함
        verify(jobService, never()).markSendFailed(anyLong());
    }

    @Test
    @DisplayName("1, 2번째 실패 후 3번째 성공을 테스트합니다.")
    void sendWithRetry_retryThenSuccess_shouldNotMarkFailed() {
        // given: 1,2번째 실패, 3번째 성공
        SqsException sqsException = (SqsException) SqsException.builder()
                .message("temporary error")
                .build();

        SendMessageResponse response = SendMessageResponse.builder()
                .messageId("msg-2")
                .build();

        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(sqsException)
                .thenThrow(sqsException)
                .thenReturn(response);

        // when & then
        assertDoesNotThrow(() -> jobPublisher.sendWithRetry(jobMessage));

        // 총 3번 호출 (2번 실패, 1번 성공)
        verify(sqsClient, times(3)).sendMessage(any(SendMessageRequest.class));
        // 최종 성공이므로 SEND_FAILED 마킹은 호출되면 안 됨
        verify(jobService, never()).markSendFailed(anyLong());
    }

    @Test
    @DisplayName("모든 재시도가 실패하면 복구 메서드가 호출되고 작업이 실패로 표시됩니다.")
    void sendWithRetry_allRetriesFail_shouldCallRecoverAndMarkFailed() {
        // given: 세 번 모두 실패
        SqsException sqsException = (SqsException) SqsException.builder()
                .message("permanent error")
                .build();

        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(sqsException)
                .thenThrow(sqsException)
                .thenThrow(sqsException);

        // when & then
        // Recover에서 예외를 다시 던지지 않으면 여기서는 예외 없이 끝남
        assertDoesNotThrow(() -> jobPublisher.sendWithRetry(jobMessage));

        // maxAttempts=3 → 3번 호출
        verify(sqsClient, times(3)).sendMessage(any(SendMessageRequest.class));
        // 모든 재시도 실패 → SEND_FAILED 마킹 1번 호출
        verify(jobService, times(1))
                .markSendFailed(jobMessage.getJobId());
    }

}
