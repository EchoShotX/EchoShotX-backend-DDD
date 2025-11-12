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
    private Credentials credentials;
    private S3 s3;

    @Getter @Setter
    public static class Sqs {
        private String queueUrl;
        private boolean fifo;
        private String messageGroupId;
    }

    @Getter @Setter
    public static class Credentials {
        private String accessKey;
        private String secretKey;
    }

    @Getter @Setter
    public static class S3 {
        private String bucket;
    }
}