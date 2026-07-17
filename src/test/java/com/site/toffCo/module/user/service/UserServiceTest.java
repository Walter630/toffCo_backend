package com.site.toffCo.module.user.service;

import com.site.toffCo.infra.security.TokenService;
import com.site.toffCo.module.auth.service.RefreshTokenService;
import com.site.toffCo.module.user.dto.RegisterEvent;
import com.site.toffCo.module.user.dto.UserRequestDTO;
import com.site.toffCo.module.user.dto.UserResponseDTO;
import com.site.toffCo.module.user.entity.Role;
import com.site.toffCo.module.user.entity.User;
import com.site.toffCo.module.user.mapper.UserMapper;
import com.site.toffCo.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RegisterProducer registerProducer;

    @InjectMocks
    private UserService service;

    @Test
    @DisplayName("Deve criar usuário no sistema")
    void createUser() {
        // Arrange
        UUID userId = UUID.randomUUID();

        UserRequestDTO dto = new UserRequestDTO(
                userId,
                "walter@gmail.com",
                "sajdansnd",
                "888888888",
                "nome",
                Role.USER
        );

        User user = new User();
        user.setId(userId);
        user.setEmail("walter@gmail.com");
        user.setPassword("sajdansnd");

        UserResponseDTO responseEsperado = new UserResponseDTO(
                userId,
                "walter@gmail.com",
                "nome"
        );

        Mockito.when(userMapper.toEntity(dto))
                .thenReturn(user);

        /*
         * O UserService provavelmente usa saveAndFlush().
         * O Mockito devolve null quando um método não foi configurado.
         */
        Mockito.when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mockito.when(userMapper.toDto(any(User.class)))
                .thenReturn(responseEsperado);

        // Act
        UserResponseDTO resultado = service.CreateUser(dto);

        // Assert
        assertNotNull(resultado);
        assertEquals(userId, resultado.userId());
        assertEquals("walter@gmail.com", resultado.email());
        assertEquals("nome", resultado.username());

        Mockito.verify(userMapper)
                .toEntity(dto);

        Mockito.verify(userRepository)
                .saveAndFlush(any(User.class));

        Mockito.verify(userMapper)
                .toDto(any(User.class));

        Mockito.verify(registerProducer)
                .send(any(RegisterEvent.class));
    }

    @Test
    void refreshToken() {
    }

    @Test
    void findAll() {
    }
}