package com.medibook.provider.resource;
 
import com.medibook.provider.dto.request.ProviderRegistrationRequest;
import com.medibook.provider.dto.request.UpdateProviderRequest;
import com.medibook.provider.dto.response.ProviderResponse;
import com.medibook.provider.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.Map;
 
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@Tag(name = "Providers",
     description = "Provider profile management, search, verification, and rating")
public class ProviderResource {
 
    private final ProviderService providerService;
 
    // ── POST /api/v1/providers — Register provider profile ────────────────
 
    @PostMapping
    @Operation(summary = "Register a new provider profile (PROVIDER role only)")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public ResponseEntity<ProviderResponse> register(
            @Valid @RequestBody ProviderRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.registerProvider(request));
    }
 
    // ── GET /api/v1/providers — Get all providers (public) ────────────────
 
    @GetMapping
    @Operation(summary = "Get all provider profiles (public — guests can browse)")
    public ResponseEntity<List<ProviderResponse>> getAll() {
        return ResponseEntity.ok(providerService.getAllProviders());
    }
 
    // ── GET /api/v1/providers/verified — Get all verified providers ────────
 
    @GetMapping("/verified")
    @Operation(summary = "Get all admin-verified providers")
    public ResponseEntity<List<ProviderResponse>> getVerified() {
        return ResponseEntity.ok(providerService.getVerifiedProviders());
    }
 
    // ── GET /api/v1/providers/{providerId} — Get by provider ID ───────────
 
    @GetMapping("/{providerId}")
    @Operation(summary = "Get provider profile by providerId")
    public ResponseEntity<ProviderResponse> getById(@PathVariable Long providerId) {
        return ResponseEntity.ok(providerService.getProviderById(providerId));
    }
 
    // ── GET /api/v1/providers/user/{userId} — Get by userId ───────────────
 
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get provider profile by userId (from auth-service)")
    public ResponseEntity<ProviderResponse> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(providerService.getProviderByUserId(userId));
    }
 
    // ── GET /api/v1/providers/specialization/{spec} — Filter by spec ──────
 
    @GetMapping("/specialization/{specialization}")
    @Operation(summary = "Get providers by specialization (public)")
    public ResponseEntity<List<ProviderResponse>> getBySpecialization(
            @PathVariable String specialization) {
        return ResponseEntity.ok(providerService.getBySpecialization(specialization));
    }
 
    // ── GET /api/v1/providers/search?q=... — Full-text search ─────────────
 
    @GetMapping("/search")
    @Operation(summary = "Search providers by name, specialization, or clinic (public)")
    public ResponseEntity<List<ProviderResponse>> search(
            @RequestParam String q) {
        return ResponseEntity.ok(providerService.searchProviders(q));
    }
 
    // ── GET /api/v1/providers/filter — Advanced filter ────────────────────
 
    @GetMapping("/filter")
    @Operation(summary = "Filter verified providers by specialization, location, rating (public)")
    public ResponseEntity<List<ProviderResponse>> filter(
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double minRating) {
        return ResponseEntity.ok(
                providerService.filterProviders(specialization, location, minRating));
    }
 
    // ── GET /api/v1/providers/count?specialization=... ────────────────────
 
    @GetMapping("/count")
    @Operation(summary = "Count providers by specialization")
    public ResponseEntity<Map<String, Integer>> countBySpecialization(
            @RequestParam String specialization) {
        int count = providerService.countBySpecialization(specialization);
        return ResponseEntity.ok(Map.of("specialization", specialization.length(),
                "count", count));
    }
 
    // ── PUT /api/v1/providers/{providerId} — Update profile ───────────────
 
    @PutMapping("/{providerId}")
    @Operation(summary = "Update provider profile (PROVIDER or ADMIN)")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public ResponseEntity<ProviderResponse> update(
            @PathVariable Long providerId,
            @Valid @RequestBody UpdateProviderRequest request) {
        return ResponseEntity.ok(providerService.updateProvider(providerId, request));
    }
 
    // ── PUT /api/v1/providers/{providerId}/verify — Admin verifies ─────────
 
    @PutMapping("/{providerId}/verify")
    @Operation(summary = "Admin: verify provider credentials and approve listing")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> verify(@PathVariable Long providerId) {
        providerService.verifyProvider(providerId);
        return ResponseEntity.ok(Map.of(
                "message", "Provider " + providerId + " has been verified successfully."));
    }
 
    // ── PUT /api/v1/providers/{providerId}/reject — Admin rejects ──────────
 
    @PutMapping("/{providerId}/reject")
    @Operation(summary = "Admin: reject / unverify a provider")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> reject(@PathVariable Long providerId) {
        providerService.rejectProvider(providerId);
        return ResponseEntity.ok(Map.of(
                "message", "Provider " + providerId + " has been rejected."));
    }
 
    // ── PUT /api/v1/providers/{providerId}/availability — Toggle availability
 
    @PutMapping("/{providerId}/availability")
    @Operation(summary = "Set provider availability (PROVIDER or ADMIN)")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> setAvailability(
            @PathVariable Long providerId,
            @RequestParam boolean available) {
        providerService.setAvailability(providerId, available);
        return ResponseEntity.ok(Map.of(
                "message", "Availability set to " + available
                        + " for providerId: " + providerId));
    }
 
    // ── PUT /api/v1/providers/{providerId}/rating — Update avg rating ──────
 
    @PutMapping("/{providerId}/rating")
    @Operation(summary = "Update provider average rating (called by review-service)")
    public ResponseEntity<Map<String, String>> updateRating(
            @PathVariable Long providerId,
            @RequestParam double rating) {
        providerService.updateRating(providerId, rating);
        return ResponseEntity.ok(Map.of(
                "message", "Rating updated to " + rating
                        + " for providerId: " + providerId));
    }
 
    // ── DELETE /api/v1/providers/{providerId} — Delete provider ───────────
 
    @DeleteMapping("/{providerId}")
    @Operation(summary = "Delete provider profile (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long providerId) {
        providerService.deleteProvider(providerId);
        return ResponseEntity.ok(Map.of(
                "message", "Provider " + providerId + " deleted successfully."));
    }
}