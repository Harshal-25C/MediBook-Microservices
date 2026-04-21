package com.medibook.provider.resource;
 
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medibook.provider.client.UserClient;
import com.medibook.provider.dto.request.ProviderRequest;
import com.medibook.provider.dto.response.ProviderDetailResponse;
import com.medibook.provider.dto.response.UserDto;
import com.medibook.provider.entity.Provider;
import com.medibook.provider.service.ProviderService;

import jakarta.validation.Valid;

/**
 * REST Controller for Provider.
 *
 * Microservices change: UserRepository replaced with UserClient (Feign).
 * UserClient calls auth-service GET /auth/profile/{userId} to get
 * fullName, email, phone, profilePicUrl for ProviderDetailResponse.
 *
 * All endpoint URLs stay identical — frontend zero changes.
 */
@RestController
@RequestMapping("/providers")
public class ProviderResource {

    @Autowired
    private ProviderService providerService;

    /**
     * Replaces direct @Autowired UserRepository.
     * Calls auth-service via Feign: GET /auth/profile/{userId}
     */
    @Autowired
    private UserClient userClient;

    @PostMapping("/register")
    public ResponseEntity<?> registerProvider(@Valid @RequestBody ProviderRequest request) {
        Provider provider = providerService.registerProvider(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Provider profile created successfully. Waiting for admin verification.",
                        "providerId", provider.getProviderId(),
                        "isVerified", provider.isVerified()
                ));
    }

    @GetMapping("/{providerId}")
    public ResponseEntity<ProviderDetailResponse> getById(@PathVariable int providerId) {
        Provider provider = providerService.getProviderById(providerId);

        // Feign call → auth-service GET /auth/profile/{userId}
        // Replaces: userRepository.findByUserId(provider.getUserId())
        UserDto user = getUserSafe(provider.getUserId(), providerId);

        ProviderDetailResponse detail = buildDetailResponse(provider, user);
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Provider> getByUserId(@PathVariable int userId) {
        return ResponseEntity.ok(providerService.getProviderByUserId(userId));
    }

    @GetMapping("/specialization/{specialization}")
    public ResponseEntity<List<Provider>> getBySpecialization(@PathVariable String specialization) {
        return ResponseEntity.ok(providerService.getBySpecialization(specialization));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Provider>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(providerService.searchProviders(keyword));
    }

    @GetMapping("/available")
    public ResponseEntity<List<Provider>> getAvailable() {
        return ResponseEntity.ok(providerService.getVerifiedAndAvailableProviders());
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProviderDetailResponse>> getAll() {
        List<Provider> providers = providerService.getAllProviders();

        List<ProviderDetailResponse> result = providers.stream().map(provider -> {
            // Feign call per provider → auth-service GET /auth/profile/{userId}
            UserDto user = getUserSafe(provider.getUserId(), provider.getProviderId());
            return buildDetailResponse(provider, user);
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{providerId}")
    public ResponseEntity<Provider> updateProvider(
            @PathVariable int providerId,
            @Valid @RequestBody ProviderRequest request) {
        return ResponseEntity.ok(providerService.updateProvider(providerId, request));
    }

    @PutMapping("/{providerId}/verify")
    public ResponseEntity<Provider> verifyProvider(@PathVariable int providerId) {
        Provider updatedProvider = providerService.verifyProvider(providerId);
        return ResponseEntity.ok(updatedProvider);
    }

    @PutMapping("/{providerId}/availability")
    public ResponseEntity<?> setAvailability(
            @PathVariable int providerId,
            @RequestParam boolean isAvailable) {
        providerService.setAvailability(providerId, isAvailable);
        String message = isAvailable
                ? "Doctor is now available for appointments."
                : "Doctor is now unavailable for appointments.";
        return ResponseEntity.ok(Map.of("message", message));
    }

    @DeleteMapping("/{providerId}")
    public ResponseEntity<?> deleteProvider(@PathVariable int providerId) {
        providerService.deleteProvider(providerId);
        return ResponseEntity.ok(Map.of("message", "Provider deleted successfully."));
    }

    /**
     * INTERNAL endpoint — called only by review-service via Feign.
     * Updates doctor avgRating after a review is submitted/updated/deleted.
     * URL: PUT /providers/{providerId}/rating?avgRating=4.5
     */
    @PutMapping("/{providerId}/rating")
    public ResponseEntity<?> updateRating(
            @PathVariable int providerId,
            @RequestParam double avgRating) {
        providerService.updateRating(providerId, avgRating);
        return ResponseEntity.ok(Map.of("message", "Rating updated."));
    }

    // ── Helper methods ────────────────────────────────────────────────

    /**
     * Safe Feign call — returns null UserDto instead of crashing
     * if auth-service is temporarily down.
     */
    private UserDto getUserSafe(int userId, int providerId) {
        try {
            return userClient.getUserById(userId);
        } catch (Exception e) {
            System.err.println("[ProviderResource] Could not fetch user " + userId
                    + " for provider " + providerId + ": " + e.getMessage());
            return null;
        }
    }

    private ProviderDetailResponse buildDetailResponse(Provider provider, UserDto user) {
        return ProviderDetailResponse.builder()
                .providerId(provider.getProviderId())
                .userId(provider.getUserId())
                .specialization(provider.getSpecialization())
                .qualification(provider.getQualification())
                .experienceYears(provider.getExperienceYears())
                .bio(provider.getBio())
                .clinicName(provider.getClinicName())
                .clinicAddress(provider.getClinicAddress())
                .avgRating(provider.getAvgRating())
                .isVerified(provider.isVerified())
                .isAvailable(provider.isAvailable())
                .createdAt(provider.getCreatedAt())
                .fullName(user != null ? user.getFullName() : "Provider #" + provider.getProviderId())
                .email(user != null ? user.getEmail() : "")
                .phone(user != null ? user.getPhone() : "")
                .profilePicUrl(user != null ? user.getProfilePicUrl() : "")
                .build();
    }
}