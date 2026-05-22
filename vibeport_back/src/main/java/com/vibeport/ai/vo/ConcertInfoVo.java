package com.vibeport.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConcertInfoVo {
    private int concertNo;
    private String artistNmKor;
    private String artistNmFor;
    private int concertYear;
    private int concertMonth;
    private int concertDate;
    private String concertTime;
    private String venue;
    private String tctSite;
    private String tctOpenAt;
    private String genre;
    private String emailYn;
    private String subject;
    private String content;
    private int popScore;
    private LocalDateTime regTime;
    private String sendYn;
    private String artistImageUrl;
}
