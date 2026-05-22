package com.vibeport.batch;

import com.vibeport.ai.service.ConcertPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissingConcertInfoScheduler {

    private final ConcertPipelineService concertPipelineService;

    @Value("${batch.missing-concert-info.enabled}")
    private boolean enabled;

    @Scheduled(cron = "${batch.missing-concert-info.cron}", zone = "${batch.missing-concert-info.zone}")
    public void scheduleMissingConcertInfoEnrichment() {
        if (!enabled) {
            log.debug("[MissingConcertInfoScheduler] batch is disabled.");
            return;
        }

        long start = System.currentTimeMillis();
        log.info("[MissingConcertInfoScheduler] missing concert info enrichment started");

        try {
            Map<String, Object> result = concertPipelineService.enrichFutureMissingConcertFieldsWithOpenAi();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[MissingConcertInfoScheduler] missing concert info enrichment finished - result={}, elapsed={} ms",
                    result, elapsed);
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[MissingConcertInfoScheduler] missing concert info enrichment failed - elapsed={} ms, message={}",
                    elapsed, ex.getMessage(), ex);
        }
    }
}
