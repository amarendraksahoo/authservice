package com.aks.authservice.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class AuthResponse {
	private String accessToken;
    private String tokenType;
    private long expiresIn;
    
    public AuthResponse(String token, String tokenType2, long expiresIn2) {
    	accessToken = token;
    	tokenType = tokenType2;
    	expiresIn = expiresIn2;
		// TODO Auto-generated constructor stub
	}
	
}
