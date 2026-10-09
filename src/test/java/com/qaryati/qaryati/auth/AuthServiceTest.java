package com.qaryati.qaryati.auth;

import com.qaryati.qaryati.user.Role;
import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_savesHashedPasswordAndReturnsToken() {
        when(userRepository.existsByUsername("sara")).thenReturn(false);
        when(userRepository.existsByEmail("sara@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtService.generateToken("sara", "CONTRIBUTOR")).thenReturn("token-1");

        AuthResponse response = authService.register(
                new RegisterRequest("sara", "sara@example.com", "password123"));

        assertEquals("token-1", response.token());
        assertEquals("CONTRIBUTOR", response.role());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("hashed", saved.getValue().getPasswordHash());
    }

    @Test
    void register_rejectsDuplicateUsername() {
        when(userRepository.existsByUsername("sara")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(
                new RegisterRequest("sara", "sara@example.com", "password123")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = new User("sara", "sara@example.com", "hashed", Role.CONTRIBUTOR);
        when(userRepository.findByUsername("sara")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("sara", "wrong")));
    }

    @Test
    void login_rejectsUnknownUser() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("ghost", "whatever1")));
    }

    @Test
    void login_returnsTokenForValidCredentials() {
        User user = new User("sara", "sara@example.com", "hashed", Role.VERIFIER);
        when(userRepository.findByUsername("sara")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken("sara", "VERIFIER")).thenReturn("token-2");

        AuthResponse response = authService.login(new LoginRequest("sara", "password123"));

        assertEquals("token-2", response.token());
        assertEquals("VERIFIER", response.role());
    }
}