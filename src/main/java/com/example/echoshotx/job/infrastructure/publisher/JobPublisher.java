package com.example.echoshotx.job.infrastructure.publisher;

import com.example.echoshotx.job.application.adaptor.JobAdaptor;
import com.example.echoshotx.job.application.service.JobService;
import com.example.echoshotx.job.infrastructure.dto.JobMessage;
import com.example.echoshotx.shared.config.aws.props.AwsProps;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobPublisher {

    private final SqsClient sqsClient;
    private final AwsProps awsProps;
    private final ObjectMapper objectMapper;

    //adaptor
    private final JobService jobService;

    @Retryable(
            value = { SqsException.class, RuntimeException.class },
            maxAttempts = 3, // 최대 3번 시도
            backoff = @Backoff(delay = 500L, multiplier = 2.0) //재시도 0.5초 시도, 이후 2배씩 늘어남
    )
    public void sendWithRetry(JobMessage message) {
        try {
            send(message);
        } catch (SqsException e) {
            log.warn("SQS send failed (SqsException). jobId={}, taskType={}, message={}",
                    message.getJobId(), message.getProcessingType(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.warn("SQS send failed (RuntimeException). jobId={}, taskType={}, message={}",
                    message.getJobId(), message.getProcessingType(), e.getMessage());
            throw e;
        }
    }

    // 전부 실패했을 때 후처리
    @Recover
    public void recover(RuntimeException e, JobMessage message) {
        log.error("SQS send permanently failed after retries. jobId={}", message.getJobId(), e);

        // 실패한 job 상태 업데이트
        jobService.markSendFailed(message.getJobId());

        // 추가로 구현할 수 있는 것들
        // 1. 슬랙/이메일 알람
        // 아래는 리팩토링 가능한 것들
        // 1. 큐에 기록 → 나중에 수동 재처리

    }

    public void send(JobMessage message) {
        String body = generateMessageBody(message);

        SendMessageRequest.Builder builder = SendMessageRequest.builder()
                .queueUrl(awsProps.getSqs().getQueueUrl())
                .messageBody(body);

        // FIFO면 groupId 필수
        if (awsProps.getSqs().isFifo()) {
            builder.messageGroupId(awsProps.getSqs().getMessageGroupId());
            // 필요시 deduplicationId도 넣을 수 있음
            // builder.messageDeduplicationId(UUID.randomUUID().toString());
        }

        // 선택: message attribute에 타입 정보 넣기
        Map<String, MessageAttributeValue> attrs = new HashMap<>();
        attrs.put("taskType", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(message.getProcessingType())
                .build());
        builder.messageAttributes(attrs);

        sqsClient.sendMessage(builder.build());
        log.info("SQS message sent: {}", body);
    }

    private String generateMessageBody(JobMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize SQS message", e);
        }
    }

}
