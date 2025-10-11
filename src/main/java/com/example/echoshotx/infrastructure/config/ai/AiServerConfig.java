package com.example.echoshotx.infrastructure.config.ai;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class AiServerConfig {
    
    @Value("${ai.server.base-url:http://ai-server:8000}")
    private String aiServerBaseUrl;
    
    @Value("${ai.server.connection-timeout:30000}")
    private int connectionTimeout;
    
    @Value("${ai.server.read-timeout:1800000}") // 30분
    private int readTimeout;
    
    @Value("${ai.server.write-timeout:30000}")
    private int writeTimeout;
    
    @Value("${ai.server.max-connections:100}")
    private int maxConnections;
    
    @Bean
    public WebClient aiServerWebClient() {
        // 커넥션 풀 설정
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ai-server-pool")
                .maxConnections(maxConnections)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .evictInBackground(Duration.ofSeconds(120))
                .build();
        
        // HTTP 클라이언트 설정
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
                );
        
        return WebClient.builder()
                .baseUrl(aiServerBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    // 대용량 파일 처리를 위한 메모리 설정
                    configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024); // 10MB
                })
                .build();
    }
}
