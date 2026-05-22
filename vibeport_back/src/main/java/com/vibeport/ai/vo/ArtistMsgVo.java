package com.vibeport.ai.vo;

import lombok.Data;
import software.amazon.awssdk.core.pagination.sync.PaginatedResponsesIterator;

@Data
public class ArtistMsgVo {
    private int msgNo;
    private int concertYear;
    private int concertMonth;
    private int concertDate;
    private String artistNmKor;
    private String artistNmFor;
    private String artistImageUrl;
    private String artistImagePath;
    private String subject;
    private String content;
    private String sendYn;
    private String regTime;
}
