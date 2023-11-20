package com.welcome.enjoytrip.util;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

//import com.ssafy.vue.exception.UnAuthorizedException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JWTUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token.expiretime}")
    private long accessTokenExpireTiem;

    @Value("${jwt.refresh-token.expiretime}")
    private long refreshTokenExpireTime;

    public String createAccessToken(String user_id) {
        return create(user_id, "access-token", accessTokenExpireTiem);
    }

    //	AccessToken에 비해 유효기간을 길게 설정.
    public String createRefreshToken(String userId) {
        return create(userId, "refresh-token", refreshTokenExpireTime);
    }

    //	Token 발급
//		key : Claim에 셋팅될 key 값
//		value : Claim에 셋팅 될 data 값
//		subject : payload에 sub의 value로 들어갈 subject값
//		expire : 토큰 유효기간 설정을 위한 값
//		jwt 토큰의 구성 : header + payload + signature
    private String create(String user_id, String subject, long expireTime) {
//		Payload 설정 : 생성일 (IssuedAt), 유효기간 (Expiration),
//		토큰 제목 (Subject), 데이터 (Claim) 등 정보 세팅.
        Claims claims = Jwts.claims()
                .setSubject(subject) // 토큰 제목 설정 ex) access-token, refresh-token
                .setIssuedAt(new Date()) // 생성일 설정
                .setExpiration(new Date(System.currentTimeMillis() + expireTime)); // 만료일 설정 (유효기간)

//		저장할 data의 key, value
        claims.put("userId", user_id);

        String jwt = Jwts.builder()
                .setHeaderParam("typ", "JWT").setClaims(claims) // Header 설정 : 토큰의 타입, 해쉬 알고리즘 정보 세팅.
                .signWith(SignatureAlgorithm.HS256, this.generateKey()) // Signature 설정 : secret key를 활용한 암호화.
                .compact(); // 직렬화 처리.

        return jwt;
    }

    //	Signature 설정에 들어갈 key 생성.
    // create 에서 호출해서 payload 부분을 담당한다
    private byte[] generateKey() {
        byte[] key = null;
        try {
//			charset 설정 안하면 사용자 플랫폼의 기본 인코딩 설정으로 인코딩 됨.
            key = secretKey.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            if (log.isInfoEnabled()) {
                e.printStackTrace();
            } else {
                log.error("Making JWT Key Error ::: {}", e.getMessage());
            }
        }
        return key;
    }

    //	전달 받은 토큰이 제대로 생성된것인지 확인 하고 문제가 있다면 UnauthorizedException을 발생.
    // 전달 받은 토큰이 유효한 키값을 가지고 있었다면 true 출력, 아니라면 false 출력
    public boolean checkToken(String token) {
        try {
//			Json Web Signature? 서버에서 인증을 근거로 인증정보를 서버의 private key로 서명 한것을 토큰화 한것
//			setSigningKey : JWS 서명 검증을 위한  secret key 세팅
//			parseClaimsJws : 파싱하여 원본 jws 만들기
            Jws<Claims> claims = Jwts.parser().setSigningKey(this.generateKey()).parseClaimsJws(token);
//			Claims 는 Map의 구현체 형태
            log.debug("claims: {}", claims);
            return true;

        } catch (Exception e) { // 이리로 오면 유효하지 않은 토큰이라는 것이다
            log.error(e.getMessage());
            return false;
        }
    }

    // 인증 토큰 값에서 user의 id 값을 출력한다
    public String getUserId(String authorization) {
        Jws<Claims> claims = null;
        try {
            claims = Jwts.parser().setSigningKey(this.generateKey()).parseClaimsJws(authorization);
        } catch (Exception e) {
            log.error(e.getMessage());
//            throw new UnAuthorizedException();
        }
        Map<String, Object> value = claims.getBody();
        log.info("value : {}", value);
        return (String) value.get("userId");
    }

}
