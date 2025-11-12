package com.example.echoshotx.shared.config.aws;

import com.example.echoshotx.shared.config.aws.props.AwsProps;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class AwsSqsConfig {

    private final AwsProps awsProps;

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.of(awsProps.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .region(Region.of(awsProps.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                awsProps.getCredentials().getAccessKey(),
                                awsProps.getCredentials().getSecretKey())
                ))
                .build();
    }

    @Bean
    public SqsTemplate sqsTemplate() {
        return SqsTemplate.newTemplate(sqsAsyncClient());
    }

    //todo refactor 필요시 커스터마이징
    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory() {
        return SqsMessageListenerContainerFactory
                .builder()
                .configure(sqsContainerOptionsBuilder ->
                        sqsContainerOptionsBuilder
                                .maxConcurrentMessages(10) // 컨테이너의 스레드 풀 크기
                                .maxMessagesPerPoll(10) // 한 번의 폴링 요청으로 수신할 수 있는 최대 메시지 수
                                .acknowledgementInterval(Duration.ofSeconds(5)) // AWS SQS 응답 간격
                                .acknowledgementThreshold(10) // AWS SQS 응답 최소 개수
                )
                .sqsAsyncClient(sqsAsyncClient())
                .build();
    }

}
