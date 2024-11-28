package com.dalhousie.servicehub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "aws.s3")
public class AwsProperties {

    public String accountId;
    public String bucketName;
    public String region;
    public String accessKey;
    public String secretKey;
    public String sessionToken;
}
