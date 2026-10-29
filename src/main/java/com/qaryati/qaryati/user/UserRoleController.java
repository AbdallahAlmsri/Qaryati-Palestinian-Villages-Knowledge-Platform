package com.qaryati.qaryati.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserRoleResponse> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        User updated = userRoleService.updateRole(userId, request.role());

        return ResponseEntity.ok(new UserRoleResponse(
                updated.getId(),
                updated.getUsername(),
                updated.getRole()
        ));
    }

    public record UserRoleResponse(
            Long id,
            String username,
            Role role
    ) {
    }
}
