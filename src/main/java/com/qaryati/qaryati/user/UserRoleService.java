package com.qaryati.qaryati.user;

import com.qaryati.qaryati.audit.AuditLogService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRoleService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public UserRoleService(UserRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public User updateRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (newRole == null) {
            throw new InvalidUserRoleException("Role cannot be null");
        }

        String currentUsername = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        if (user.getUsername().equals(currentUsername)
                && user.getRole() == Role.ADMIN
                && newRole != Role.ADMIN) {
            throw new UserRoleConflictException("An admin cannot remove their own admin role");
        }

        Role oldRole = user.getRole();
        user.changeRole(newRole);
        User saved = userRepository.save(user);

        auditLogService.record("ROLE_CHANGE", "USER", userId, oldRole.name(), newRole.name());
        return saved;
    }
}