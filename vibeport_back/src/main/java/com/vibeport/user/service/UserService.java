package com.vibeport.user.service;

import com.vibeport.ai.vo.ArtistMsgVo;
import com.vibeport.auth.enums.Tokens;
import com.vibeport.auth.utils.JwtUtil;
import com.vibeport.exception.handler.BusinessException;
import com.vibeport.mail.service.ArtistMsgMailService;
import com.vibeport.mail.service.TestEmailService;
import com.vibeport.mail.service.VerificationMailService;
import com.vibeport.user.mapper.UserMapper;
import com.vibeport.user.vo.RatingVo;
import com.vibeport.user.vo.UserVo;
import com.vibeport.user.vo.VerifCodeVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final VerificationMailService verificationMailService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ArtistMsgMailService emailService;

    String TOKEN_PREFIX = "Bearer ";

    public UserVo getUserProfile(String userId) {
        // 사용자 프로필 정보를 조회하는 로직 구현
        return this.userMapper.selectUserProfile(userId);
    }

    public RatingVo getUserRatings(String userId) {
        // 사용자 평점 정보를 조회하는 로직 구현
        return this.userMapper.selectUserRatings(userId);
    }

    /*
     * 인증 코드 전송
     */
    public void sendVerificationCode(UserVo userVo) {

        String email = userVo.getEmail();

        if(email == null || email.isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수 항목입니다.");
        }

        // 존재하는 이메일인지 확인
        boolean isExistEmail = this.userMapper.checkEmailExists(email);
//
        if(isExistEmail) {
            // TODO: BuisinessException으로 변경 필요
            throw new BusinessException("이미 가입된 이메일입니다.");
        }

        // 이전에 발송된 인증 코드 삭제
        this.userMapper.deletePreVerifCode(email);

        // 인증 코드 생성
        String verificationCode = this.generateVerificationCode();

        // 인증코드 이메일 전송
        VerifCodeVo codeVo = new VerifCodeVo();
        codeVo.setCode(verificationCode);
        codeVo.setEmailList(List.of(email));
        codeVo.setEmail(email);
        this.verificationMailService.verifyEmailSend(codeVo);

         //인증 코드 저장
        this.userMapper.insertEmailVerificationCodes(codeVo);

        System.out.println("verificationCode==========" + verificationCode);
    }

    /*
     * 인증 코드 생성
     */
    private String generateVerificationCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        int codeLength = 6;
        SecureRandom random = new SecureRandom();

        StringBuilder code = new StringBuilder(codeLength);

        for(int i=0; i < codeLength; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }

        return code.toString();
    }

    /*
     * 입력한 코드와 발송한 인증 코드 확인
     */
    public void verifyCode(VerifCodeVo codeVo) {
        // 이메일, 입력한 코드로 올바른 인증 코드인지 확인
        boolean isRightCode = this.userMapper.selectIsRightCode(codeVo);

        if(!isRightCode) {
            // TODO: BuisinessException으로 변경 필요
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        }
    }

    /*
     * 회원 가입
     */
    public void join(UserVo userVo) {
        String email = userVo.getEmail();
        String password = userVo.getPassword();

        if(email == null || email.isEmpty()) {
            // TODO: BuisinessException으로 변경 필요
            throw new IllegalArgumentException("이메일은 필수 항목입니다.");
        }

        if(password == null || password.isEmpty()) {
            // TODO: BuisinessException으로 변경 필요
            throw new IllegalArgumentException("비밀번호는 필수 항목입니다.");
        }

        // 이메일 중복 확인
        boolean isExistEmail = this.userMapper.checkEmailExists(email);
        if(isExistEmail) {
            // TODO: BuisinessException으로 변경 필요
            throw new BusinessException("이미 가입된 이메일입니다.");
        }

        // 회원 ID는 이메일 주소의 '@' 앞부분으로 설정
        String userName = String.valueOf(userVo.getEmail()).split("@")[0];
        userVo.setUserName(userName);

        // 비밀번호 암호화
        userVo.setPassword(passwordEncoder.encode(userVo.getPassword())); //비밀번호를 암호화

        // 고유 회원 번호 생성
        userVo.setUserNo(UUID.randomUUID().toString());

        // 회원 정보 저장
        this.userMapper.insertUser(userVo);
    }

    public String reissue(String refresh) throws Exception {
        UserVo userVo = new UserVo();
        userVo.setEmail(this.jwtUtil.getEmailFromToken(refresh));
        userVo.setUserNo(this.jwtUtil.getUserNoFromToken(refresh));
        userVo.setRole(this.jwtUtil.getRoleFromToken(refresh));

        return this.jwtUtil.createToken(Tokens.ACCESS.getValue(), userVo);
    }

    public void validRefreshToken(String refresh) {
        this.jwtUtil.validToken(refresh, Tokens.REFRESH.getValue());
    }


    public void registEmail(Map<String, Object> param) throws Exception {
        // 인증시 사용한 이메일인지 확인
        boolean isCorrectEmail = this.userMapper.isCorrectEmail(param);

        if(!isCorrectEmail) {
            throw new IllegalArgumentException("이메일 주소를 확인해주세요.");
        }

        // 이메일 등록
        this.userMapper.insertEmail(param);

        // 온보딩 이메일 전송
        this.sendOnBoardingEmail(param);
    }

    public ArtistMsgVo getLatestLetter() {
        return this.userMapper.selectLatestLatter();
    }

    public List<ArtistMsgVo> getSentLetters() {
        return this.userMapper.selectSentLetters();
    }

    /*
     * 온보딩 이메일 전송
     */
    private void sendOnBoardingEmail(Map<String, Object> param) {

        // 가장 최근 뉴스레터
//        ArtistMsgVo artistMsgVo = this.userMapper.selectLatestLatter();
//        param.put("latestMsg", artistMsgVo);

        // 온보딩 이메일 전송
        this.emailService.sendOnboardingEmail(param);
    }
}
