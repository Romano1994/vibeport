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
}
