package com.site.toffCo.module.user.service;

import com.site.toffCo.infra.security.TokenService;
import com.site.toffCo.module.auth.entity.RefreshToken;
import com.site.toffCo.module.auth.service.RefreshTokenService;
import com.site.toffCo.module.user.dto.LoginRequestDTO;
import com.site.toffCo.module.user.dto.LoginResponseDTO;
import com.site.toffCo.module.user.dto.UserRequestDTO;
import com.site.toffCo.module.user.dto.UserResponseDTO;
import com.site.toffCo.module.user.entity.Role;
import com.site.toffCo.module.user.entity.User;
import com.site.toffCo.module.user.mapper.UserMapper;
import com.site.toffCo.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper  userMapper;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenService tokenService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService service;

    @Test
    @DisplayName("Criando user no sistema")
    void createUser() {
        //preparar o cenario
        UserRequestDTO dto = new UserRequestDTO("walter@gmail.com", "sajdansnd", "888888888", "nome", Role.USER);
        User user = new User();
        user.setEmail("walter@gmail.com");
        user.setPassword("sajdansnd");

        UserResponseDTO userResponseDTO = new UserResponseDTO("walter@gmail.com", "nome");

        Mockito.when(userMapper.toEntity(dto)).thenReturn(user);
        Mockito.when(userRepository.save(user)).thenReturn(user);

        Mockito.when(userMapper.toDto(user)).thenReturn(userResponseDTO);
        //agir chama o metodo para salvar no banco
        UserResponseDTO result = service.CreateUser(dto);

        //verifica se ta tudo certo
        assertNotNull(result);
        assertEquals("walter@gmail.com", result.email());
        assertEquals("nome", result.name());

        Mockito.verify(userMapper).toEntity(dto);
        Mockito.verify(userRepository, Mockito.times(1)).save(user);
        Mockito.verify(userMapper).toDto(user);
    }

    //@Test
    //@DisplayName("deve realizar o login com sucesso")
    //void login() {
        //preparar o cenario
    //var dto = new LoginRequestDTO("walter@gmail.com", "sajdansnd");

    // String fakeAccessToken = "access-token-gerado-no-teste";
    // String fakeRefreshTokenString = "refresh-token-gerado-no-teste";

            //  var mockRefreshToken = Mockito.mock(RefreshToken.class);
            //  Mockito.when(mockRefreshToken.getToken()).thenReturn(fakeRefreshTokenString);

        // Ensinando os mocks a se comportarem:
        // 1. O authenticationManager.authenticate não retorna nada (void) quando dá certo, ele só passa direto.
        // 2. Quando pedir para gerar o Access Token, devolve nossa String falsa:
            //  Mockito.when(tokenService.generateToken(dto.email())).thenReturn(fakeAccessToken);

    //Mockito.when(refreshTokenService.createRefreshToken(fakeAccessToken)).thenReturn(mockRefreshToken);

        //vamos agir criar esse login
        // LoginResponseDTO result = service.login(dto);

        //verificar
        // assertNotNull(result);

        //assertEquals(fakeAccessToken, result.token());
            // assertEquals(fakeRefreshTokenString, result.refreshToken());
            // Mockito.verify(authenticationManager).authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class));
            // Mockito.verify(tokenService).generateToken(dto.email());
    //  Mockito.verify(refreshTokenService).createRefreshToken(fakeAccessToken);
    //}

    @Test
    void refreshToken() {
    }

    @Test
    void findAll() {
    }
}