package com.vibeport.concert.controller;

import com.vibeport.ai.vo.ConcertInfoVo;
import com.vibeport.concert.service.ConcertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/vibeport/concert/")
public class ConcertController {

    private final ConcertService concertService;

    @PostMapping("getConcertInfosByMonth")
    public List<ConcertInfoVo> getConcertInfosByMonth(@RequestBody Map<String, Object> param) throws Exception{
        return this.concertService.getConcertInfosByMonth(param);
    }

    @GetMapping("getTodayFeature")
    public ConcertInfoVo getTodayFeature() throws Exception{
        return this.concertService.getTodayFeatureConcert();
    }

    /**
     * 이미지 없는 미래 공연을 limit 건씩 보강한다. (수동 트리거)
     * body: { "limit": 10, "frYmd": "2026-06-01", "toYmd": "2026-09-30" } - 모두 선택값
     */
    @PostMapping("enrichArtistImages")
    public Map<String, Object> enrichArtistImages(@RequestBody(required = false) Map<String, Object> param) throws Exception {
        return this.concertService.enrichArtistImages(param);
    }
}
