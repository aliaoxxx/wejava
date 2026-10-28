package com.wejava.security.jwt;

import com.wejava.security.config.SecurityProperties;
import com.wejava.security.model.LoginSession;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * JWT 签发与解析。
 * Token 只携带身份标识，不携带权限列表 —— 权限从 Redis 会话里取，改权限可即时生效。
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_USERNAME = "uname";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final SecurityProperties properties;

    public String createAccessToken(LoginSession user, String jti) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(user.getUserId()))
                .id(jti)
                .issuedAt(now)
                .expiresAt(now.plus(properties.getAccessTtl()))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_USER_ID, user.getUserId())
                .claim(CLAIM_USERNAME, user.getUsername())
                .build();
        return encode(claims);
    }

    public String createRefreshToken(Long userId, String jti) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(userId))
                .id(jti)
                .issuedAt(now)
                .expiresAt(now.plus(properties.getRefreshTtl()))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_USER_ID, userId)
                .build();
        return encode(claims);
    }

    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
