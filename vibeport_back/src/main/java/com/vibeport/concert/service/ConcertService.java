package com.vibeport.concert.service;

import com.vibeport.ai.client.GeminiClient;
import com.vibeport.ai.mapper.AiMapper;
import com.vibeport.ai.vo.ConcertInfoVo;
import com.vibeport.concert.mapper.ConcertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcertService {

    private static final int DEFAULT_IMAGE_ENRICH_LIMIT = 10;
    private static final int MAX_IMAGE_ENRICH_LIMIT = 50;

    private final ConcertMapper concertMapper;
    private final AiMapper aiMapper;
    private final GeminiClient geminiClient;

    public List<ConcertInfoVo> getConcertInfosByMonth(Map<String, Object> param) {
        return this.concertMapper.selectConcertInfosByMonth(param);
    }

    public ConcertInfoVo getTodayFeatureConcert() {
        return this.concertMapper.selectTodayFeatureConcert();
    }

    /**
     * 이미지가 없는 미래 공연을 limit 건씩 골라 SerpAPI/이미지 다운로드로 보강한다.
     * 수동 트리거 전용 (Postman 등).
     */
    public Map<String, Object> enrichArtistImages(Map<String, Object> param) {
        int limit = parseLimit(param != null ? param.get("limit") : null);
        String frYmd = param != null && param.get("frYmd") != null ? param.get("frYmd").toString() : null;
        String toYmd = param != null && param.get("toYmd") != null ? param.get("toYmd").toString() : null;

        Map<String, Object> queryParam = new LinkedHashMap<>();
        queryParam.put("limit", limit);
        queryParam.put("frYmd", frYmd);
        queryParam.put("toYmd", toYmd);

        List<ConcertInfoVo> targets = aiMapper.selectConcertsMissingImage(queryParam);
        log.info("[ConcertService] enrichArtistImages - limit={}, frYmd={}, toYmd={}, targetCount={}",
                limit, frYmd, toYmd, targets.size());

        List<Integer> updatedConcertNos = new ArrayList<>();
        List<Integer> skippedConcertNos = new ArrayList<>();

        for (ConcertInfoVo concert : targets) {
            try {
                String artistImageUrl = geminiClient.getArtistPictureUrl(concert.getArtistNmFor());
                if (artistImageUrl == null || artistImageUrl.isBlank()) {
                    log.warn("[ConcertService] image not found - concertNo={}, artist={}",
                            concert.getConcertNo(), concert.getArtistNmFor());
                    skippedConcertNos.add(concert.getConcertNo());
                    continue;
                }
                int rows = aiMapper.updateConcertArtistImage(concert.getConcertNo(), artistImageUrl);
                if (rows > 0) {
                    updatedConcertNos.add(concert.getConcertNo());
                } else {
                    skippedConcertNos.add(concert.getConcertNo());
                }
            } catch (Exception e) {
                log.error("[ConcertService] image enrich failed - concertNo={}, artist={}: {}",
                        concert.getConcertNo(), concert.getArtistNmFor(), e.getMessage(), e);
                skippedConcertNos.add(concert.getConcertNo());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("limit", limit);
        result.put("frYmd", frYmd);
        result.put("toYmd", toYmd);
        result.put("targetCount", targets.size());
        result.put("updatedCount", updatedConcertNos.size());
        result.put("skippedCount", skippedConcertNos.size());
        result.put("updatedConcertNos", updatedConcertNos);
        result.put("skippedConcertNos", skippedConcertNos);
        return result;
    }

    private int parseLimit(Object raw) {
        if (raw == null) {
            return DEFAULT_IMAGE_ENRICH_LIMIT;
        }
        try {
            int value = Integer.parseInt(raw.toString().trim());
            if (value <= 0) return DEFAULT_IMAGE_ENRICH_LIMIT;
            return Math.min(value, MAX_IMAGE_ENRICH_LIMIT);
        } catch (NumberFormatException e) {
            return DEFAULT_IMAGE_ENRICH_LIMIT;
        }
    }
}
