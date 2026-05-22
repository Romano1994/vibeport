package com.vibeport.auth.mapper;

import com.vibeport.user.vo.UserVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginMapper {

    UserVo findByEmail(String email);
}
