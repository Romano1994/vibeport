package com.vibeport.auth.vo;

import com.vibeport.user.vo.UserVo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private UserVo userVo;
}
