package com.qaryati.qaryati.user;

import com.qaryati.qaryati.audit.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserRoleService userRoleService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    @Test
    void updateRole_throwsWhenUserDoesNotExist() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userRoleService.updateRole(9L, Role.VERIFIER));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void updateRole_rejectsNullRole() {
        User user = new User("sara", "sara@example.com", "hashed", Role.CONTRIBUTOR);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThrows(InvalidUserRoleException.class,
                () -> userRoleService.updateRole(7L, null));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void updateRole_rejectsAnAdminDemotingThemself() {
        User admin = new User("boss", "boss@example.com", "hashed", Role.ADMIN);
        loginAs("boss");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(UserRoleConflictException.class,
                () -> userRoleService.updateRole(1L, Role.CONTRIBUTOR));

        assertEquals(Role.ADMIN, admin.getRole());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void updateRole_changesTheRoleAndRecordsAnAuditEntry() {
        User target = new User("sara", "sara@example.com", "hashed", Role.CONTRIBUTOR);
        loginAs("boss");
        when(userRepository.findById(7L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userRoleService.updateRole(7L, Role.VERIFIER);

        assertEquals(Role.VERIFIER, result.getRole());
        verify(auditLogService).record("ROLE_CHANGE", "USER", 7L, "CONTRIBUTOR", "VERIFIER");
    }
}