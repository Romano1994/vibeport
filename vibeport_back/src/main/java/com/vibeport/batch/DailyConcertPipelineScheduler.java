package com.vibeport.batch;

import com.vibeport.ai.service.ConcertPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyConcertPipelineScheduler {

    private final ConcertPipelineService concertPipelineService;

    @Value("${batch.daily.concert-collection.enabled}")
    private boolean concertCollectionEnabled;

    @Value("${batch.daily.newsletter.enabled}")
    private boolean newsletterEnabled;

    @Scheduled(
            cron = "${batch.daily.concert-collection.cron}",
            zone = "${batch.daily.concert-collection.zone}"
    )
    public void scheduleDailyConcertCollection() {
        if (!concertCollectionEnabled) {
            log.debug("[DailyConcertPipelineScheduler] daily concert collection batch is disabled.");
            return;
        }

        runPipelineStep("daily concert collection", concertPipelineService::collectDailyConcerts);
    }

    @Scheduled(
            cron = "${batch.daily.newsletter.cron}",
            zone = "${batch.daily.newsletter.zone}"
    )
    public void scheduleDailyNewsletter() {
        if (!newsletterEnabled) {
            log.debug("[DailyConcertPipelineScheduler] daily newsletter batch is disabled.");
            return;
        }

        runPipelineStep("daily newsletter", concertPipelineService::sendDailyNewsletter);
    }

    public void scheduleDailyPipeline() {
        scheduleDailyConcertCollection();
        scheduleDailyNewsletter();
    }

    private void runPipelineStep(String batchName, Runnable pipelineStep) {
        long start = System.currentTimeMillis();
        log.info("[DailyConcertPipelineScheduler] {} batch started", batchName);

        try {
            pipelineStep.run();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[DailyConcertPipelineScheduler] {} batch finished - elapsed={} ms", batchName, elapsed);
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[DailyConcertPipelineScheduler] {} batch failed - elapsed={} ms, message={}",
                    batchName, elapsed, ex.getMessage(), ex);
        }
    }
}
