package com.example.echoshotx.shared.config.aws;

import com.example.echoshotx.shared.config.aws.props.AwsProps;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@RequiredArgsConstructor
public class AwsClientConfig {

    private final AwsProps awsProps;

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.of(awsProps.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

}
