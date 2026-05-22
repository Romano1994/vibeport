package com.vibeport.user.controller;

import com.vibeport.ai.vo.ArtistMsgVo;
import com.vibeport.auth.enums.Tokens;
import com.vibeport.user.service.UserService;
import com.vibeport.user.vo.RatingVo;
import com.vibeport.user.vo.UserVo;
import com.vibeport.user.vo.VerifCodeVo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.reactor.ReactorEnvironmentPostProcessor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/vibeport/user/")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("getUserProfile")
    public UserVo getUserProfile(HttpServletRequest request, HttpServletResponse response) throws Exception{
        // 유저 세션 체크하는 로직

        // 유저 이메일 추출

        String userId = "exampleUserId"; // 예시로 고정된 사용자 ID 사용
        return this.userService.getUserProfile(userId);
    }

    @GetMapping("getUserRatings")
    public RatingVo getUserRatings(HttpServletRequest request, HttpServletResponse response) throws Exception{
        // 유저 세션 체크하는 로직

        // 유저 이메일 추출

        String userId = "exampleUserId"; // 예시로 고정된 사용자 ID 사용
        return this.userService.getUserRatings(userId);
    }

    @GetMapping("getLatestLetter")
    public ArtistMsgVo getLatestLetter() {
        return this.userService.getLatestLetter();
    }

    @GetMapping("getSentLetters")
    public List<ArtistMsgVo> getSentLetters() {
        return this.userService.getSentLetters();
    }

    /*
     * 인증 코드 전송
     */
    @PostMapping("sendVerificationCode")
    public ResponseEntity sendVerificationCode(@RequestBody UserVo userVo) throws Exception{
        // TODO : 추후 실행 정도에 따라 다른 쓰레드에서 실행되도록 변경 필요
        // 인증 코드 전송 로직
        this.userService.sendVerificationCode(userVo);

        return ResponseEntity.ok().build();
    }

    /*
     * 인증 코드 확인
     */
    @PostMapping("verifyCode")
    public ResponseEntity verifyCode(@RequestBody VerifCodeVo codeVo) throws Exception{
        // 인증 코드 확인
        this.userService.verifyCode(codeVo);

        return ResponseEntity.ok().build();
    }

    /*
     * 이메일 등록
     */
    @PostMapping("registEmail")
    public ResponseEntity registEmail(@RequestBody Map<String, Object> param) throws Exception {
        // 이메일 등록
        this.userService.registEmail(param);

        return ResponseEntity.ok().build();
    }

    /*
     * 회원 가입
     */
    @PostMapping("join")
    public ResponseEntity join(@RequestBody UserVo userVo) throws Exception{
        // 회원 가입
        this.userService.join(userVo);

        return ResponseEntity.ok().build();
    }

    @PostMapping("reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) throws Exception{

        try {
            Cookie[] cookies = request.getCookies();
            String refresh = "";

            if(!ObjectUtils.isEmpty(cookies)) {
                for(Cookie cookie : cookies) {
                    String cookieNm = cookie.getName();
                    if(cookieNm.equals(Tokens.REFRESH.getValue())) {
                        refresh = cookie.getValue();
                    }
                }
            }

            if(ObjectUtils.isEmpty(refresh)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("");
            }

            // refresh 토큰 검사
            this.userService.validRefreshToken(refresh);
            String newAccess = this.userService.reissue(refresh);

            HttpHeaders headers = new HttpHeaders();
            headers.add("authorization", newAccess);

            return ResponseEntity.ok().headers(headers).body("access reissue");
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("access reissue error");
        }


    }
}
