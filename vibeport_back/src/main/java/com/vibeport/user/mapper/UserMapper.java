package com.vibeport.user.mapper;

import com.vibeport.ai.vo.ArtistMsgVo;
import com.vibeport.user.vo.RatingVo;
import com.vibeport.user.vo.UserVo;
import com.vibeport.user.vo.VerifCodeVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    UserVo selectUserProfile(String userId);

    RatingVo selectUserRatings(String userId);

    /*
     * 이메일 존재 여부 확인
     */
    boolean checkEmailExists(String email);

    /*
     * 인증 코드 DB 저장
     */
    void insertEmailVerificationCodes(VerifCodeVo codeVo);

    /*
     * 올바른 인증 코드인지 확인
     */
    boolean selectIsRightCode(VerifCodeVo codeVo);

    /*
     * 사용자 저장
     */
    void insertUser(UserVo userVo);

    void deletePreVerifCode(String email);

    boolean isCorrectEmail(Map<String, Object> param);

    void insertEmail(Map<String, Object> param);

    ArtistMsgVo selectLatestLatter();

    List<ArtistMsgVo> selectSentLetters();
}
