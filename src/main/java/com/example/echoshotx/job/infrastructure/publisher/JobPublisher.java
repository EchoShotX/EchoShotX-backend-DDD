package com.example.echoshotx.job.infrastructure.publisher;

import com.example.echoshotx.shared.config.aws.props.AwsProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobPublisher {

    private final SqsClient sqsClient;
    private final AwsProps awsProps;
    private final ObjectMapper objectMapper;

    public void send() {

    }

}
