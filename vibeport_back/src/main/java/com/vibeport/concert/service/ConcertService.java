package com.vibeport.concert.service;

import com.vibeport.ai.vo.ConcertInfoVo;
import com.vibeport.concert.mapper.ConcertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertMapper concertMapper;

    public List<ConcertInfoVo> getConcertInfosByMonth(Map<String, Object> param) {
        return this.concertMapper.selectConcertInfosByMonth(param);
    }

    public ConcertInfoVo getTodayFeatureConcert() {
        return this.concertMapper.selectTodayFeatureConcert();
    }
}
