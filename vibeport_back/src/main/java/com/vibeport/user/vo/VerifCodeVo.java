package com.vibeport.user.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class VerifCodeVo {

    private String code;
    private String email;
    private List<String> emailList;
    private int cnt;
    private Date regTime;
}
