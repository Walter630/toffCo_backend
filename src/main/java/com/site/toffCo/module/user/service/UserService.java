package com.site.toffCo.module.user.service;

import com.site.toffCo.infra.exception.user.EmailIsExisting;
import com.site.toffCo.infra.exception.user.InvalidRefreshToken;
import com.site.toffCo.infra.security.TokenService;
import com.site.toffCo.module.auth.service.RefreshTokenService;
import com.site.toffCo.module.user.dto.RegisterEvent;
import com.site.toffCo.module.auth.dto.LoginRequestDTO;
import com.site.toffCo.module.auth.dto.LoginResponseDTO;
import com.site.toffCo.module.auth.dto.RefreshTokenDTO;
import com.site.toffCo.module.user.dto.UserRequestDTO;
import com.site.toffCo.module.user.dto.UserResponseDTO;
import com.site.toffCo.module.auth.entity.RefreshToken;
import com.site.toffCo.module.user.entity.Role;
import com.site.toffCo.module.user.mapper.UserMapper;
import com.site.toffCo.module.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final RegisterProducer registerProducer;

    public UserService(UserRepository repository, RefreshTokenService refresh , UserMapper mapper,  TokenService tokenService,  AuthenticationManager authenticationManager, RegisterProducer registerProducer) {
        this.repository = repository;
        this.mapper = mapper;
        this.tokenService = tokenService ;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refresh;
        this.registerProducer = registerProducer;
    }

    //============================== CREATE \ REGISTER ==============================

    @Transactional
    public UserResponseDTO CreateUser(UserRequestDTO userRequestDTO) {
        if(repository.findByEmail(userRequestDTO.email()).isPresent()) {
            log.info("Email existente para o produto: {}", userRequestDTO.email());
            throw new EmailIsExisting("E-mail já cadastrado!"); // Ou seu ExceptionHandler personalizado
        }
        var userCriado = mapper.toEntity(userRequestDTO);
        log.info("Criado: {}", userCriado);
        log.info("id: {}", userCriado.getId());
        // 2. Define o cargo padrão automaticamente ANTES de salvar no banco.
        // ATENÇÃO: Troque 'Role.USER' pelo nome exato da sua classe Enum e do valor.
        // (ex: UserRole.CLIENTE, RoleEnum.DEFAULT_USER, et
        userCriado.setRole(Role.USER);
        var userSave = this.repository.save(userCriado);
        log.info("User criado: {}", userCriado);
        registerProducer.send(new RegisterEvent(userSave.getEmail(), userSave.getUsername()));
        return mapper.toDto(userSave);
    }

    //============================== LOGIN ==============================

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        var user = new UsernamePasswordAuthenticationToken(loginRequestDTO.email(), loginRequestDTO.password());
        this.authenticationManager.authenticate(user);
        var userCreate = repository.findByEmail(loginRequestDTO.email())
                .orElseThrow(() -> new RuntimeException("Erro critico"));
        var accessToken = tokenService.generateToken(loginRequestDTO.email());

        var refreshToken = refreshTokenService.createRefreshToken(userCreate);
        log.info("Token: {}, refreshToken: {}", accessToken, refreshToken);
        return new LoginResponseDTO(accessToken, refreshToken.getToken());
    }


    //============================== REFRESH ==============================

    @Transactional
    public LoginResponseDTO refreshToken(RefreshTokenDTO refreshTokenDTO) {
        return refreshTokenService.findByToken(refreshTokenDTO.refreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newAccessToken = tokenService.generateToken(user.getEmail());
                    return new LoginResponseDTO(refreshTokenDTO.refreshToken(), newAccessToken);
                })
                .orElseThrow(() -> new InvalidRefreshToken("Refresh token invalid"));
    }

    //============================== LISTUSER ==============================

    public List<UserResponseDTO> findAll() {
        return mapper.toDtoList(repository.findAll());
    }
}
