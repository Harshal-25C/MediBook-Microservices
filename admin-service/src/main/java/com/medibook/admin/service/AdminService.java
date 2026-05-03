package com.medibook.admin.service;

import com.medibook.admin.dto.AddAdminRequest;
import com.medibook.admin.dto.UserResponse;

import java.util.List;

public interface AdminService {

    /** Return all users in the system */
    List<UserResponse> getAllUsers();

    /** Return users filtered by role (Patient / Provider / Admin) */
    List<UserResponse> getUsersByRole(String role);

    /** Return a single user by ID */
    UserResponse getUserById(int userId);

    /** Deactivate (soft-delete) a user */
    void deactivateUser(int userId);

    /** Reactivate a previously deactivated user */
    void reactivateUser(int userId);

    /** Add a brand-new Admin account at runtime (without restarting) */
    UserResponse addAdmin(AddAdminRequest request);

    /** List all admin accounts */
    List<UserResponse> getAllAdmins();
}
