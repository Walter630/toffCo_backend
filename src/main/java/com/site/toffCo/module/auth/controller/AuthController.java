package com.site.toffCo.module.auth.controller;

import com.site.toffCo.module.auth.dto.RefreshTokenDTO;
import com.site.toffCo.module.auth.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auths")
public class AuthController {

    private final RefreshTokenService service;

    public AuthController(RefreshTokenService service) {
        this.service = service;
    }

}
