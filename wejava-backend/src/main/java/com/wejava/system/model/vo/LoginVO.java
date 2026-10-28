package com.wejava.system.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String tokenType = "Bearer";
}
