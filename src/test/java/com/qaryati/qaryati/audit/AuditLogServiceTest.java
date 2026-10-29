package com.qaryati.qaryati.audit;

import com.qaryati.qaryati.user.Role;
import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.user.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void record_savesAuditLogWithAuthenticatedActor() {
        User actor = new User(
                "verifier1",
                "verifier1@example.com",
                "hashed",
                Role.VERIFIER
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "verifier1", null, List.of()
                )
        );

        when(userRepository.findByUsername("verifier1"))
                .thenReturn(Optional.of(actor));

        auditLogService.record(
                "REVIEW",
                "POPULATION_RECORD",
                10L,
                "PENDING",
                "VERIFIED"
        );

        verify(auditLogRepository).save(argThat(log ->
                log.getActor() == actor
                        && "REVIEW".equals(log.getAction())
                        && "POPULATION_RECORD".equals(log.getEntityType())
                        && Long.valueOf(10L).equals(log.getEntityId())
                        && "PENDING".equals(log.getOldValue())
                        && "VERIFIED".equals(log.getNewValue())
                        && log.getCreatedAt() != null
        ));
    }

    @Test
    void record_savesAuditLogWithoutActorWhenUnauthenticated() {
        auditLogService.record(
                "REVIEW",
                "LANDMARK",
                20L,
                "PENDING",
                "REJECTED"
        );

        verify(auditLogRepository).save(argThat(log ->
                log.getActor() == null
                        && "REVIEW".equals(log.getAction())
                        && "LANDMARK".equals(log.getEntityType())
                        && Long.valueOf(20L).equals(log.getEntityId())
                        && "PENDING".equals(log.getOldValue())
                        && "REJECTED".equals(log.getNewValue())
        ));

        verifyNoInteractions(userRepository);
    }

    @Test
    void record_doesNotSaveWhenRepositoryFails() {
        doThrow(new RuntimeException("Database error"))
                .when(auditLogRepository)
                .save(any(AuditLog.class));

        assertThrows(RuntimeException.class, () ->
                auditLogService.record(
                        "REVIEW",
                        "VILLAGE_NAME",
                        30L,
                        "PENDING",
                        "VERIFIED"
                )
        );
    }
}