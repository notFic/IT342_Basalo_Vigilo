package edu.cit.basalo.vigilo.features.auth;

import jakarta.validation.constraints.NotBlank;

public class GoogleOAuthRequest {
    
    @NotBlank(message = "ID Token is required")
    private String idToken;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
