package com.vibeport.batch;

import com.vibeport.ai.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiConcertScheduler {

    private final GeminiService geminiService;

    @Value("${batch.gemini.concert.enabled}")
    private boolean enabled;

    /**
     * GeminiService.fetchAndNotifyNewConcerts 주기적 실행
     * cron 표현식은 application.properties 의 batch.gemini.concert.cron 으로 외부 설정합니다.
     */
    @Scheduled(cron = "${batch.gemini.concert.cron}")
    public void scheduleFetchAndNotifyNewConcerts() {
        if (!enabled) {
            log.debug("[GeminiConcertScheduler] 배치가 비활성화되어 있어 실행하지 않습니다.");
            return;
        }

        long start = System.currentTimeMillis();
        log.info("[GeminiConcertScheduler] fetchAndNotifyNewConcerts 배치 시작");

        try {
            geminiService.fetchAndNotifyNewConcerts();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[GeminiConcertScheduler] 배치 성공 - 수행 시간: {} ms", elapsed);
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[GeminiConcertScheduler] 배치 실행 중 예외 발생 - 수행 시간: {} ms, message: {}", elapsed, ex.getMessage(), ex);
        }
    }
}
