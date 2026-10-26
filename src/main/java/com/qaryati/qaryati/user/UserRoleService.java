package com.qaryati.qaryati.user;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class UserRoleService {

    private final UserRepository userRepository;

    public UserRoleService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User updateRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        if (newRole == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Role cannot be null"
            );
        }

        String currentUsername = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        if (user.getUsername().equals(currentUsername)
                && user.getRole() == Role.ADMIN
                && newRole != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An admin cannot remove their own admin role"
            );
        }

        user.changeRole(newRole);

        return userRepository.save(user);
    }
}
