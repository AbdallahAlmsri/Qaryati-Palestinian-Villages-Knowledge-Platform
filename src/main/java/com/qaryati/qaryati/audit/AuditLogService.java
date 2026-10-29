package com.qaryati.qaryati.audit;

import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String entityType, Long entityId,
                       String oldValue, String newValue) {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        User actor = null;

        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();

            actor = userRepository.findByUsername(username).orElse(null);
        }

        AuditLog log = new AuditLog(
                actor,
                action,
                entityType,
                entityId,
                oldValue,
                newValue
        );

        auditLogRepository.save(log);
    }
}
