package com.vibeport.ai.service;

import com.vibeport.ai.client.GeminiClient;
import com.vibeport.ai.client.OpenAiClient;
import com.vibeport.ai.mapper.AiMapper;
import com.vibeport.ai.vo.ArtistMsgVo;
import com.vibeport.ai.vo.ConcertInfoVo;
import com.vibeport.mail.service.ArtistMsgMailService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConcertPipelineServiceTests {

    private static final String UNKNOWN = new String(new char[] {(char) 48120, (char) 51221});

    @Test
    void findMissingConcertFieldsIncludesConfiguredUnknownValues() {
        ConcertPipelineService service = service(mock(GeminiClient.class), null, null);
        ConcertInfoVo concert = new ConcertInfoVo();
        concert.setConcertTime("0000");
        concert.setVenue("");
        concert.setTctSite(UNKNOWN);
        concert.setTctOpenAt(null);

        Set<String> missingFields = service.findMissingConcertFields(concert);

        assertEquals(Set.of("concertTime", "venue", "tctSite", "tctOpenAt"), missingFields);
    }

    @Test
    void enrichMissingConcertFieldsUpdatesOnlyOriginallyMissingFields() {
        GeminiClient geminiClient = mock(GeminiClient.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        ConcertPipelineService service = service(geminiClient, openAiClient, null);

        ConcertInfoVo concert = new ConcertInfoVo();
        concert.setArtistNmKor("Test Artist");
        concert.setConcertYear(2026);
        concert.setConcertMonth(7);
        concert.setConcertDate(10);
        concert.setConcertTime("0000");
        concert.setVenue("");
        concert.setTctSite("NOL Ticket");
        concert.setTctOpenAt(UNKNOWN);

        ConcertInfoVo enriched = new ConcertInfoVo();
        enriched.setConcertTime("1900");
        enriched.setVenue("KSPO DOME");
        enriched.setTctSite("YES24 Ticket");
        enriched.setTctOpenAt("2026-06-01 20:00");
        when(openAiClient.fetchMissingConcertFields(any(ConcertInfoVo.class), anySet())).thenReturn(enriched);

        service.enrichMissingConcertFields(List.of(concert));

        assertEquals("1900", concert.getConcertTime());
        assertEquals("KSPO DOME", concert.getVenue());
        assertEquals("NOL Ticket", concert.getTctSite());
        assertEquals("2026-06-01 20:00", concert.getTctOpenAt());
        verify(openAiClient).fetchMissingConcertFields(any(ConcertInfoVo.class), anySet());
        verify(geminiClient, never()).fetchMissingConcertFields(any(ConcertInfoVo.class), anySet());
    }

    @Test
    void enrichMissingConcertFieldsKeepsUnknownWhenAiReturnsNull() {
        GeminiClient geminiClient = mock(GeminiClient.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        ConcertPipelineService service = service(geminiClient, openAiClient, null);

        ConcertInfoVo concert = new ConcertInfoVo();
        concert.setConcertTime("0000");
        concert.setVenue("");
        concert.setTctSite(null);
        concert.setTctOpenAt(UNKNOWN);
        when(openAiClient.fetchMissingConcertFields(any(ConcertInfoVo.class), anySet())).thenReturn(null);

        service.enrichMissingConcertFields(List.of(concert));

        assertEquals(UNKNOWN, concert.getConcertTime());
        assertEquals(UNKNOWN, concert.getVenue());
        assertEquals(UNKNOWN, concert.getTctSite());
        assertEquals(UNKNOWN, concert.getTctOpenAt());
        verify(openAiClient).fetchMissingConcertFields(any(ConcertInfoVo.class), anySet());
        verify(geminiClient, never()).fetchMissingConcertFields(any(ConcertInfoVo.class), anySet());
    }

    @Test
    void enrichMissingConcertFieldsSkipsAiCallWhenNoFieldsAreMissing() {
        GeminiClient geminiClient = mock(GeminiClient.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        ConcertPipelineService service = service(geminiClient, openAiClient, null);

        ConcertInfoVo concert = new ConcertInfoVo();
        concert.setConcertTime("1900");
        concert.setVenue("KSPO DOME");
        concert.setTctSite("NOL Ticket");
        concert.setTctOpenAt("2026-06-01 20:00");

        service.enrichMissingConcertFields(List.of(concert));

        verify(geminiClient, never()).fetchMissingConcertFields(any(ConcertInfoVo.class), anySet());
        verify(openAiClient, never()).fetchMissingConcertFields(any(ConcertInfoVo.class), anySet());
    }

    @Test
    void enrichMissingConcertFieldsLimitsParallelTasksToFive() {
        GeminiClient geminiClient = mock(GeminiClient.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        ConcertPipelineService service = service(geminiClient, openAiClient, null);
        ConcurrentCallTracker tracker = new ConcurrentCallTracker(5);

        when(openAiClient.fetchMissingConcertFields(any(ConcertInfoVo.class), anySet())).thenAnswer(invocation -> {
            tracker.enterAndWaitForPeers();

            ConcertInfoVo enriched = new ConcertInfoVo();
            enriched.setConcertTime("1900");
            return enriched;
        });

        service.enrichMissingConcertFields(List.of(
                missingConcert(1),
                missingConcert(2),
                missingConcert(3),
                missingConcert(4),
                missingConcert(5),
                missingConcert(6)
        ));

        assertEquals(5, tracker.maxConcurrent.get());
    }

    @Test
    void enrichFutureMissingConcertFieldsWithOpenAiLimitsParallelTasksToFiveAndKeepsCounts() {
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        AiMapper aiMapper = mock(AiMapper.class);
        ConcertPipelineService service = service(null, openAiClient, aiMapper);
        ConcurrentCallTracker tracker = new ConcurrentCallTracker(5);

        when(aiMapper.selectFutureMissingConcertInfos()).thenReturn(List.of(
                missingConcert(1),
                missingConcert(2),
                missingConcert(3),
                missingConcert(4),
                missingConcert(5),
                missingConcert(6)
        ));
        when(aiMapper.updateMissingConcertFields(any(ConcertInfoVo.class))).thenReturn(1);
        when(openAiClient.fetchMissingConcertFields(any(ConcertInfoVo.class), anySet())).thenAnswer(invocation -> {
            tracker.enterAndWaitForPeers();

            ConcertInfoVo enriched = new ConcertInfoVo();
            enriched.setConcertTime("1900");
            return enriched;
        });

        Map<String, Object> result = service.enrichFutureMissingConcertFieldsWithOpenAi();

        assertEquals(5, tracker.maxConcurrent.get());
        assertEquals(6, result.get("updatedCount"));
        assertEquals(0, result.get("skippedCount"));
        assertEquals(List.of(1, 2, 3, 4, 5, 6), result.get("updatedConcertNos"));
        assertEquals(List.of(), result.get("skippedConcertNos"));
    }

    @Test
    void collectDailyConcertsSendsNewsletterForTopPopUnsentConcertWhenNoConcertsAreCollected() throws Exception {
        GeminiClient geminiClient = mock(GeminiClient.class);
        AiMapper aiMapper = mock(AiMapper.class);
        NewsletterService newsletterService = mock(NewsletterService.class);
        ArtistMsgMailService artistMsgMailService = mock(ArtistMsgMailService.class);
        ConcertPipelineService service = service(
                geminiClient,
                null,
                aiMapper,
                newsletterService,
                artistMsgMailService
        );

        ConcertInfoVo topConcert = missingConcert(10);
        topConcert.setPopScore(95);
        ArtistMsgVo artistMsgVo = new ArtistMsgVo();
        artistMsgVo.setArtistNmKor(topConcert.getArtistNmKor());
        artistMsgVo.setArtistNmFor(topConcert.getArtistNmFor());
        artistMsgVo.setConcertYear(topConcert.getConcertYear());
        artistMsgVo.setConcertMonth(topConcert.getConcertMonth());
        artistMsgVo.setConcertDate(topConcert.getConcertDate());
        artistMsgVo.setSubject("Top pop concert");
        artistMsgVo.setContent("Newsletter content");

        when(geminiClient.getConcertInfos(anyInt(), anyInt())).thenReturn(List.of());
        when(aiMapper.selectTopPopUnsentConcert()).thenReturn(topConcert);
        when(newsletterService.generate(topConcert)).thenReturn(artistMsgVo);
        when(aiMapper.selectEmailList()).thenReturn(List.of("test@example.com"));

        service.collectDailyConcerts();

        verify(newsletterService).generate(topConcert);
        verify(aiMapper).insertArtistMsg(artistMsgVo);
        verify(artistMsgMailService).artistMsgEmailSend(List.of("test@example.com"), artistMsgVo);
        verify(aiMapper).updateArtistMsgSendYn(artistMsgVo);
        verify(aiMapper).updateConcertInfoSendYn(artistMsgVo);
    }

    @Test
    void sendDailyNewsletterFetchesMissingArtistImageBeforeSavingArtistMsg() throws Exception {
        GeminiClient geminiClient = mock(GeminiClient.class);
        AiMapper aiMapper = mock(AiMapper.class);
        NewsletterService newsletterService = mock(NewsletterService.class);
        ArtistMsgMailService artistMsgMailService = mock(ArtistMsgMailService.class);
        ConcertPipelineService service = service(
                geminiClient,
                null,
                aiMapper,
                newsletterService,
                artistMsgMailService
        );

        ConcertInfoVo topConcert = missingConcert(20);
        topConcert.setArtistNmFor("Coldplay");
        topConcert.setArtistImageUrl(null);
        when(aiMapper.selectTopPopUnsentConcert()).thenReturn(topConcert);
        when(geminiClient.getArtistPictureUrl("Coldplay")).thenReturn("https://example.com/coldplay.jpg");
        when(newsletterService.generate(topConcert)).thenAnswer(invocation -> {
            ConcertInfoVo concert = invocation.getArgument(0);
            ArtistMsgVo artistMsgVo = new ArtistMsgVo();
            artistMsgVo.setArtistNmKor(concert.getArtistNmKor());
            artistMsgVo.setArtistNmFor(concert.getArtistNmFor());
            artistMsgVo.setConcertYear(concert.getConcertYear());
            artistMsgVo.setConcertMonth(concert.getConcertMonth());
            artistMsgVo.setConcertDate(concert.getConcertDate());
            artistMsgVo.setSubject("Top pop concert");
            artistMsgVo.setContent("Newsletter content");
            artistMsgVo.setArtistImageUrl(concert.getArtistImageUrl());
            return artistMsgVo;
        });
        when(aiMapper.selectEmailList()).thenReturn(List.of());

        service.sendDailyNewsletter();

        assertEquals("https://example.com/coldplay.jpg", topConcert.getArtistImageUrl());
        verify(geminiClient).getArtistPictureUrl("Coldplay");
        verify(aiMapper).mergeConcertInfo(topConcert);
        verify(aiMapper).insertArtistMsg(any(ArtistMsgVo.class));
    }

    private ConcertPipelineService service(GeminiClient geminiClient, OpenAiClient openAiClient, AiMapper aiMapper) {
        return new ConcertPipelineService(geminiClient, openAiClient, aiMapper, null, null, null);
    }

    private ConcertPipelineService service(
            GeminiClient geminiClient,
            OpenAiClient openAiClient,
            AiMapper aiMapper,
            NewsletterService newsletterService,
            ArtistMsgMailService artistMsgMailService
    ) {
        return new ConcertPipelineService(
                geminiClient,
                openAiClient,
                aiMapper,
                newsletterService,
                artistMsgMailService,
                null
        );
    }

    private ConcertInfoVo missingConcert(int concertNo) {
        ConcertInfoVo concert = new ConcertInfoVo();
        concert.setConcertNo(concertNo);
        concert.setArtistNmKor("Test Artist " + concertNo);
        concert.setConcertYear(2026);
        concert.setConcertMonth(7);
        concert.setConcertDate(10);
        concert.setConcertTime("0000");
        concert.setVenue("KSPO DOME");
        concert.setTctSite("NOL Ticket");
        concert.setTctOpenAt("2026-06-01 20:00");
        return concert;
    }

    private static class ConcurrentCallTracker {
        private final CountDownLatch allStarted;
        private final AtomicInteger currentConcurrent = new AtomicInteger();
        private final AtomicInteger maxConcurrent = new AtomicInteger();

        private ConcurrentCallTracker(int expectedCalls) {
            this.allStarted = new CountDownLatch(expectedCalls);
        }

        private void enterAndWaitForPeers() throws InterruptedException {
            int current = currentConcurrent.incrementAndGet();
            maxConcurrent.updateAndGet(previous -> Math.max(previous, current));
            allStarted.countDown();
            allStarted.await(500, TimeUnit.MILLISECONDS);
            currentConcurrent.decrementAndGet();
        }
    }
}
