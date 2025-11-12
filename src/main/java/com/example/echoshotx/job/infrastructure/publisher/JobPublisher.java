package com.example.echoshotx.job.infrastructure.publisher;

import com.example.echoshotx.job.infrastructure.dto.JobMessage;
import com.example.echoshotx.shared.config.aws.props.AwsProps;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobPublisher {

    private final SqsClient sqsClient;
    private final AwsProps awsProps;
    private final ObjectMapper objectMapper;

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
                .stringValue(message.getTaskType())
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
