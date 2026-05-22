package com.vibeport.commonVo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorLogVo {

    private Long id;
    private String uri;
    private String methodNm;
    private String msg;
    private String stackTrace;
    private String clientIp;
    private LocalDateTime timestamp;

}
