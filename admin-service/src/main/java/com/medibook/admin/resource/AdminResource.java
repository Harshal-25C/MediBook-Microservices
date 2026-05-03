package com.medibook.admin.resource;

import com.medibook.admin.dto.AddAdminRequest;
import com.medibook.admin.dto.UserResponse;
import com.medibook.admin.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * All endpoints here require a valid JWT with role = "Admin".
 * Security is enforced in SecurityConfig via .hasAuthority("Admin").
 *
 * To call any endpoint:
 *   1. Login via POST http://localhost:8080/auth/login
 *      (using harshalchoudhary340@gmail.com or adityalandge64@gmail.com)
 *   2. Copy the returned token
 *   3. Add header: Authorization: Bearer <token>
 *   4. Call any /admin/** endpoint through the API Gateway (port 8080)
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminResource {

    private final AdminService adminService;

    // ── User Management ────────────────────────────────────────────────────

    /**
     * GET /admin/users
     * Returns every user in the system (all roles).
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    /**
     * GET /admin/users/{userId}
     * Returns a single user by ID.
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable int userId) {
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    /**
     * GET /admin/users/role/{role}
     * Returns users filtered by role: Patient | Provider | Admin
     */
    @GetMapping("/users/role/{role}")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(adminService.getUsersByRole(role));
    }

    /**
     * PUT /admin/users/{userId}/deactivate
     * Soft-deletes (deactivates) a user so they can no longer log in.
     */
    @PutMapping("/users/{userId}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable int userId) {
        adminService.deactivateUser(userId);
        return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
    }

    /**
     * PUT /admin/users/{userId}/reactivate
     * Re-enables a previously deactivated user account.
     */
    @PutMapping("/users/{userId}/reactivate")
    public ResponseEntity<?> reactivateUser(@PathVariable int userId) {
        adminService.reactivateUser(userId);
        return ResponseEntity.ok(Map.of("message", "User reactivated successfully"));
    }

    // ── Admin Management ───────────────────────────────────────────────────

    /**
     * GET /admin/admins
     * Lists all admin accounts (useful to verify who has admin access).
     */
    @GetMapping("/admins")
    public ResponseEntity<List<UserResponse>> getAllAdmins() {
        return ResponseEntity.ok(adminService.getAllAdmins());
    }

    /**
     * POST /admin/admins
     * Adds a brand-new admin account at runtime — no restart required.
     *
     * Body:
     * {
     *   "fullName": "New Admin",
     *   "email":    "newadmin@example.com",
     *   "password": "SecurePass@123"
     * }
     *
     * Only an existing admin (you) can call this endpoint.
     */
    @PostMapping("/admins")
    public ResponseEntity<?> addAdmin(@Valid @RequestBody AddAdminRequest request) {
        UserResponse created = adminService.addAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Admin created successfully",
                        "userId",  created.getUserId(),
                        "email",   created.getEmail()
                ));
    }

    // ── Health / Info ──────────────────────────────────────────────────────

    /**
     * GET /admin/ping
     * Quick health check — confirms the service is running and token is valid.
     */
    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of(
                "status",  "UP",
                "service", "admin-service",
                "message", "Admin service is running"
        ));
    }
}
