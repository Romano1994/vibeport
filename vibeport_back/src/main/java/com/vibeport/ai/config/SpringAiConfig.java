package com.vibeport.ai.config;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Configuration
public class SpringAiConfig {

    @Bean
    @Primary
    public WebClient webClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofMinutes(5))
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                ))
                .codecs(configure -> configure.defaultCodecs().maxInMemorySize(1024 * 1024 * 10))
                .build();
    }
}
