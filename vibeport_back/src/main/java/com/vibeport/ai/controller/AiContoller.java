package com.vibeport.ai.controller;

import com.vibeport.ai.mapper.AiMapper;
import com.vibeport.ai.service.ConcertPipelineService;
import com.vibeport.ai.service.GeminiService;
import com.vibeport.ai.vo.ConcertInfoVo;
import com.vibeport.ai.vo.NewsletterTestRequestVo;
import com.vibeport.batch.DailyConcertPipelineScheduler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/vibeport/ai/")
@RequiredArgsConstructor
public class AiContoller {

    private final GeminiService aiService;
    private final DailyConcertPipelineScheduler scheduler;
    private final ConcertPipelineService concertPipelineService;
    private final AiMapper aiMapper;

    @GetMapping("getConcertInfo")
    public void updateConcertInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
        this.scheduler.scheduleDailyPipeline();
    }

    /**
     * 뉴스레터 생성 및 발송 테스트.
     *
     * 요청 예시 1 — DB 공연 데이터 사용:
     * { "email": "test@example.com" }
     *
     * 요청 예시 2 — 아티스트 직접 입력:
     * {
     *   "email": "test@example.com",
     *   "artistNmKor": "콜드플레이",
     *   "artistNmFor": "Coldplay",
     *   "concertYear": 2025,
     *   "concertMonth": 4,
     *   "concertDate": 25,
     *   "concertTime": "19:00",
     *   "venue": "잠실 올림픽주경기장",
     *   "tctSite": "인터파크",
     *   "tctOpenAt": "2025-02-10 12:00",
     *   "genre": "팝록"
     * }
     */
    @PostMapping("test/newsletter")
    public ResponseEntity<Map<String, Object>> testNewsletter(@RequestBody NewsletterTestRequestVo req) {
        log.info("[테스트 API] 뉴스레터 테스트 요청 - email: {}, artist: {}",
                req.getEmail(), req.getArtistNmFor());
        try {
            Map<String, Object> result = concertPipelineService.testGenerateAndSend(req);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("[테스트 API] 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[테스트 API] 뉴스레터 테스트 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("test/missing-concert-info")
    public ResponseEntity<Map<String, Object>> testMissingConcertInfoEnrichment() {
        log.info("[Test API] missing concert info enrichment requested");
        try {
            Map<String, Object> result = concertPipelineService.enrichFutureMissingConcertFieldsWithOpenAi();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[Test API] missing concert info enrichment failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
