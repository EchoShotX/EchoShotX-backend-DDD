package com.example.echoshotx.shared.config.aws.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cloud.aws")
public class AwsProps {
    private String region;
    private Sqs sqs;

    @Getter @Setter
    public static class Sqs {
        private String queueUrl;
        private boolean fifo;
        private String messageGroupId;
    }
}