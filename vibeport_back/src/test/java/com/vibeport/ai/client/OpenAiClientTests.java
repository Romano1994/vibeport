package com.vibeport.ai.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiClientTests {

    @Test
    void openAiClientUsesExpectedModelFallbacks() throws Exception {
        Field newsletterModel = OpenAiClient.class.getDeclaredField("model");
        Field missingFieldModel = OpenAiClient.class.getDeclaredField("responsesModel");

        assertThat(newsletterModel.getAnnotation(org.springframework.beans.factory.annotation.Value.class).value())
                .isEqualTo("${openai.chat.model:gpt-5.4-mini}");
        assertThat(missingFieldModel.getAnnotation(org.springframework.beans.factory.annotation.Value.class).value())
                .isEqualTo("${openai.responses.model:gpt-5.4-nano}");
    }

    @Test
    void generateNewsletterUsesGpt5CompatibleChatCompletionTokenLimitParameter() {
        AtomicReference<String> requestBody = new AtomicReference<>();
        ExchangeFunction exchangeFunction = request -> {
            MockClientHttpRequest mockRequest = new MockClientHttpRequest(HttpMethod.POST, URI.create("/chat/completions"));
            request.body().insert(mockRequest, new BodyInserter.Context() {
                @Override
                public List<HttpMessageWriter<?>> messageWriters() {
                    return ExchangeStrategies.withDefaults().messageWriters();
                }

                @Override
                public Optional<ServerHttpRequest> serverRequest() {
                    return Optional.empty();
                }

                @Override
                public Map<String, Object> hints() {
                    return Map.of();
                }
            }).block();
            requestBody.set(mockRequest.getBodyAsString().block());

            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                            {
                              "choices": [
                                {
                                  "message": {
                                    "content": "{\\"subject\\":\\"hello\\",\\"content\\":\\"world\\"}"
                                  }
                                }
                              ]
                            }
                            """)
                    .build());
        };
        OpenAiClient client = new OpenAiClient(WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build());
        ReflectionTestUtils.setField(client, "model", "gpt-5.4-mini");

        client.generateNewsletter("system JSON", "user");

        assertThat(requestBody.get()).contains("\"max_completion_tokens\":2000");
        assertThat(requestBody.get()).doesNotContain("\"max_tokens\"");
        assertThat(requestBody.get()).doesNotContain("\"temperature\"");
    }

    @Test
    void generateNewsletterRetriesTooManyRequestsAndReturnsSuccessfulResponse() {
        AtomicInteger attempts = new AtomicInteger();
        ExchangeFunction exchangeFunction = request -> {
            if (attempts.incrementAndGet() == 1) {
                return Mono.just(ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"error\":{\"message\":\"rate limit\"}}")
                        .build());
            }
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                            {
                              "choices": [
                                {
                                  "message": {
                                    "content": "{\\"subject\\":\\"hello\\",\\"content\\":\\"world\\"}"
                                  }
                                }
                              ]
                            }
                            """)
                    .build());
        };
        OpenAiClient client = new OpenAiClient(WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build());
        ReflectionTestUtils.setField(client, "model", "gpt-test");
        ReflectionTestUtils.setField(client, "newsletterRetryMaxAttempts", 2);
        ReflectionTestUtils.setField(client, "newsletterRetryBackoffMs", 1L);
        ReflectionTestUtils.setField(client, "newsletterRetryMaxBackoffMs", 1L);

        Map<String, String> result = client.generateNewsletter("system", "user");

        assertThat(result).containsEntry("subject", "hello")
                .containsEntry("content", "world");
        assertThat(attempts).hasValue(2);
    }
}
