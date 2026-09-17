package com.taskora.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.taskora.backend.auth.dto.AuthResponse;
import com.taskora.backend.auth.dto.LoginRequest;
import com.taskora.backend.auth.dto.RegisterRequest;
import com.taskora.backend.exception.EmailAlreadyUsedException;
import com.taskora.backend.exception.InvalidCredentialsException;
import com.taskora.backend.security.JwtService;
import com.taskora.backend.user.User;
import com.taskora.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    private AuthService newAuthService() {
        return new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void register_shouldCreateUserAndReturnToken_whenEmailNotUsed() {
        authService = newAuthService();
        RegisterRequest request = new RegisterRequest("jane@doe.com", "password123", "Jane", "Doe");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("fake-jwt");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("jane@doe.com");
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyUsed() {
        authService = newAuthService();
        RegisterRequest request = new RegisterRequest("jane@doe.com", "password123", "Jane", "Doe");

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        authService = newAuthService();
        LoginRequest request = new LoginRequest("jane@doe.com", "password123");
        User user = User.builder()
                .id(1L)
                .email("jane@doe.com")
                .password("hashed")
                .firstName("Jane")
                .lastName("Doe")
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("fake-jwt");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("fake-jwt");
        assertThat(response.email()).isEqualTo("jane@doe.com");
    }

    @Test
    void login_shouldThrowInvalidCredentials_whenAuthenticationFails() {
        authService = newAuthService();
        LoginRequest request = new LoginRequest("jane@doe.com", "wrong-password");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

}
