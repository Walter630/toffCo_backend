package com.site.toffCo.module.user.controller;

import com.site.toffCo.module.auth.dto.RefreshTokenDTO;
import com.site.toffCo.module.login.dto.LoginRequestDTO;
import com.site.toffCo.module.login.dto.LoginResponseDTO;
import com.site.toffCo.module.user.dto.UserRequestDTO;
import com.site.toffCo.module.user.dto.UserResponseDTO;
import com.site.toffCo.module.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    //============================== REGISTER ==============================

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.CreateUser(dto));
    }

    //============================== LOGIN ==============================

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        var response = service.login(dto);
        return ResponseEntity.ok().body(response);
    }

    //============================== REFRESH TOKEN ==============================

    @PostMapping("/refresh_token")
    public ResponseEntity<LoginResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        var response = service.refreshToken(dto);
        return ResponseEntity.ok().body(response);
    }

    //============================== LISTUSER ==============================

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }
}
