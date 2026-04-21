package com.medibook.provider.service;
 
import com.medibook.provider.dto.request.ProviderRegistrationRequest;
import com.medibook.provider.dto.request.UpdateProviderRequest;
import com.medibook.provider.dto.response.ProviderResponse;
 
import java.util.List;
 
public interface ProviderService {
 
    // ── From PDF Section 4.2 — ProviderServiceImpl methods ───────────────
 
    ProviderResponse registerProvider(ProviderRegistrationRequest request);
 
    ProviderResponse getProviderById(Long providerId);
 
    ProviderResponse getProviderByUserId(Long userId);
 
    List<ProviderResponse> getBySpecialization(String specialization);
 
    List<ProviderResponse> searchProviders(String query);
 
    List<ProviderResponse> filterProviders(String specialization,
                                           String location,
                                           Double minRating);
 
    ProviderResponse updateProvider(Long providerId, UpdateProviderRequest request);
 
    void verifyProvider(Long providerId);
 
    void rejectProvider(Long providerId);
 
    void setAvailability(Long providerId, boolean isAvailable);
 
    void deleteProvider(Long providerId);
 
    void updateRating(Long providerId, double newAvgRating);
 
    List<ProviderResponse> getAllProviders();
 
    List<ProviderResponse> getVerifiedProviders();
 
    int countBySpecialization(String specialization);
}