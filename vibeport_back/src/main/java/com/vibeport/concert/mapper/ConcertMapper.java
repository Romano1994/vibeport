package com.vibeport.concert.mapper;

import com.vibeport.ai.vo.ConcertInfoVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ConcertMapper {

    List<ConcertInfoVo> selectConcertInfosByMonth(Map<String, Object> param);

    ConcertInfoVo selectTodayFeatureConcert();
}
