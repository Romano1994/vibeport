package com.vibeport.ai.vo;

import lombok.Data;

@Data
public class NewsletterTestRequestVo {
    // 필수: 테스트 발송 대상 이메일
    private String email;

    // 선택: 직접 입력 시 사용. 비어 있으면 DB에서 최고 인기 공연 조회
    private String artistNmKor;
    private String artistNmFor;
    private Integer concertYear;
    private Integer concertMonth;
    private Integer concertDate;
    private String concertTime;
    private String venue;
    private String tctSite;
    private String tctOpenAt;
    private String genre;
}
