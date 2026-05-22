package com.vibeport.auth.utils;

import com.vibeport.auth.enums.Tokens;
import com.vibeport.user.vo.UserVo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.access}")
    private long accessExp;

    @Value("${jwt.expiration.refresh}")
    private long refreshExp;

    private SecretKey key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String subject, UserVo userVo) {
        Date now = new Date();
        Date exp;

        if(subject.equals(Tokens.ACCESS.getValue())) {
            exp = new Date(now.getTime() + accessExp);
        } else if(subject.equals(Tokens.REFRESH.getValue())) {
            exp = new Date(now.getTime() + refreshExp);
        } else {
            throw new RuntimeException("토큰 발급 에러");
        }

        return Jwts.builder()
                .claim("category", subject)
//                .claim("email", userVo.getEmail())
//                .claim("userNo", userVo.getUserNo())
//                .claim("role", userVo.getRole())
                .claim("email", "fhaksh0369@gmail.com")
                .claim("userNo", "123123")
                .claim("role", "User")
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createToken(String subject) {
        Date now = new Date();
        Date exp;

        if(subject.equals("access")) {
            exp = new Date(now.getTime() + accessExp);
        } else if(subject.equals("refresh")) {
            exp = new Date(now.getTime() + refreshExp);
        } else {
            throw new RuntimeException("토큰 발급 에러");
        }

        return Jwts.builder()
                .claim("category", subject)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String value) {
        // TODO - 배포 시 strict로 변경 필요
        String sameSite = "Lax";

        String category = Tokens.REFRESH.getValue();

        return  String.format("%s=%s; Path=%s; Max-Age=%d; HttpOnly; Secure=%s; SameSite=%s",
                category, value, "/", (int)refreshExp/1000, "false", sameSite);
    }

    public Claims parseClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // subject 일치 및 만료 체크
    public boolean validToken(String token, String subject) {
        try {
            Claims claims = this.parseClaims(token);
            return subject.equals(claims.getSubject()) && claims.getExpiration().after(new Date());
        } catch(JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUserNoFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("userNo", String.class);
    }

    public String getRoleFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("role", String.class);
    }


    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("email", String.class);
    }
}
