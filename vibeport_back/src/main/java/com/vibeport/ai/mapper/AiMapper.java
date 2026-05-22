package com.vibeport.ai.mapper;

import com.vibeport.ai.vo.ConcertInfoVo;
import com.vibeport.ai.vo.ArtistMsgVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiMapper {
    void mergeConcertInfo(ConcertInfoVo data);

    void insertMailLog(Map<String, Object> paramMap);

    void insertArtistMsg(ArtistMsgVo artistMsgVo);

    List<ConcertInfoVo> selectSavedList(Map<String, Object> paramMap);

    ConcertInfoVo selectConcertInfoByNo(@Param("concertNo") int concertNo);

    List<ConcertInfoVo> selectFutureMissingConcertInfos();

    int updateMissingConcertFields(ConcertInfoVo concertInfoVo);

    List<String> selectEmailList();

    void updateArtistMsgSendYn(ArtistMsgVo artistMsgVo);

    void updateConcertInfoSendYn(ArtistMsgVo artistMsgVo);

    ConcertInfoVo selectRandomSendedInfo();

    ConcertInfoVo selectTopPopUnsentConcert();
}
