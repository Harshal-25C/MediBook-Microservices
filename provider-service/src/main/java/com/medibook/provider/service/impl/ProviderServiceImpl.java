package com.medibook.provider.service.impl;
 
import com.medibook.provider.dto.request.ProviderRegistrationRequest;
import com.medibook.provider.dto.request.UpdateProviderRequest;
import com.medibook.provider.dto.response.ProviderResponse;
import com.medibook.provider.entity.Provider;
import com.medibook.provider.exception.ResourceNotFoundException;
import com.medibook.provider.repository.ProviderRepository;
import com.medibook.provider.service.ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.List;
import java.util.stream.Collectors;
 
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {
 
    private final ProviderRepository providerRepository;
 
    // ── registerProvider ──────────────────────────────────────────────────
 
    @Override
    @Transactional
    public ProviderResponse registerProvider(ProviderRegistrationRequest request) {
        if (providerRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException(
                    "Provider profile already exists for userId: " + request.getUserId());
        }
 
        Provider provider = Provider.builder()
                .userId(request.getUserId())
                .specialization(request.getSpecialization())
                .qualification(request.getQualification())
                .experienceYears(request.getExperienceYears())
                .bio(request.getBio())
                .clinicName(request.getClinicName())
                .clinicAddress(request.getClinicAddress())
                .avgRating(0.0)
                .isVerified(false)
                .isAvailable(true)
                .build();
 
        Provider saved = providerRepository.save(provider);
        log.info("Provider profile registered for userId: {}", saved.getUserId());
        return mapToResponse(saved);
    }
 
    // ── getProviderById ───────────────────────────────────────────────────
 
    @Override
    public ProviderResponse getProviderById(Long providerId) {
        return mapToResponse(findById(providerId));
    }
 
    // ── getProviderByUserId ───────────────────────────────────────────────
 
    @Override
    public ProviderResponse getProviderByUserId(Long userId) {
        Provider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provider profile not found for userId: " + userId));
        return mapToResponse(provider);
    }
 
    // ── getBySpecialization ───────────────────────────────────────────────
 
    @Override
    public List<ProviderResponse> getBySpecialization(String specialization) {
        return providerRepository
                .findBySpecializationIgnoreCase(specialization)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
 
    // ── searchProviders ───────────────────────────────────────────────────
 
    @Override
    public List<ProviderResponse> searchProviders(String query) {
        return providerRepository
                .searchByNameOrSpecialization(query)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
 
    // ── filterProviders ───────────────────────────────────────────────────
 
    @Override
    public List<ProviderResponse> filterProviders(String specialization,
                                                   String location,
                                                   Double minRating) {
        return providerRepository
                .filterProviders(specialization, location, minRating)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
 
    // ── updateProvider ────────────────────────────────────────────────────
 
    @Override
    @Transactional
    public ProviderResponse updateProvider(Long providerId, UpdateProviderRequest request) {
        Provider provider = findById(providerId);
 
        if (request.getSpecialization() != null)
            provider.setSpecialization(request.getSpecialization());
        if (request.getQualification() != null)
            provider.setQualification(request.getQualification());
        if (request.getExperienceYears() != null)
            provider.setExperienceYears(request.getExperienceYears());
        if (request.getBio() != null)
            provider.setBio(request.getBio());
        if (request.getClinicName() != null)
            provider.setClinicName(request.getClinicName());
        if (request.getClinicAddress() != null)
            provider.setClinicAddress(request.getClinicAddress());
 
        Provider updated = providerRepository.save(provider);
        log.info("Provider profile updated for providerId: {}", updated.getProviderId());
        return mapToResponse(updated);
    }
 
    // ── verifyProvider (Admin action) ─────────────────────────────────────
 
    @Override
    @Transactional
    public void verifyProvider(Long providerId) {
        Provider provider = findById(providerId);
        if (provider.getIsVerified()) {
            throw new IllegalStateException(
                    "Provider is already verified: " + providerId);
        }
        provider.setIsVerified(true);
        providerRepository.save(provider);
        log.info("Provider verified by admin — providerId: {}", providerId);
    }
 
    // ── rejectProvider (Admin action) ─────────────────────────────────────
 
    @Override
    @Transactional
    public void rejectProvider(Long providerId) {
        Provider provider = findById(providerId);
        provider.setIsVerified(false);
        provider.setIsAvailable(false);
        providerRepository.save(provider);
        log.info("Provider rejected/unverified by admin — providerId: {}", providerId);
    }
 
    // ── setAvailability ───────────────────────────────────────────────────
 
    @Override
    @Transactional
    public void setAvailability(Long providerId, boolean isAvailable) {
        Provider provider = findById(providerId);
        provider.setIsAvailable(isAvailable);
        providerRepository.save(provider);
        log.info("Provider {} availability set to {} — providerId: {}",
                provider.getUserId(), isAvailable, providerId);
    }
 
    // ── deleteProvider ────────────────────────────────────────────────────
 
    @Override
    @Transactional
    public void deleteProvider(Long providerId) {
        Provider provider = findById(providerId);
        providerRepository.delete(provider);
        log.info("Provider profile deleted — providerId: {}", providerId);
    }
 
    // ── updateRating (called by review-service after new review) ──────────
 
    @Override
    @Transactional
    public void updateRating(Long providerId, double newAvgRating) {
        Provider provider = findById(providerId);
        if (newAvgRating < 0 || newAvgRating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }
        provider.setAvgRating(newAvgRating);
        providerRepository.save(provider);
        log.info("Rating updated to {} for providerId: {}", newAvgRating, providerId);
    }
 
    // ── getAllProviders ───────────────────────────────────────────────────
 
    @Override
    public List<ProviderResponse> getAllProviders() {
        return providerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
 
    // ── getVerifiedProviders ──────────────────────────────────────────────
 
    @Override
    public List<ProviderResponse> getVerifiedProviders() {
        return providerRepository.findByIsVerified(true)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
 
    // ── countBySpecialization ─────────────────────────────────────────────
 
    @Override
    public int countBySpecialization(String specialization) {
        return providerRepository.countBySpecialization(specialization);
    }
 
    // ── Private helpers ───────────────────────────────────────────────────
 
    private Provider findById(Long providerId) {
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provider not found with id: " + providerId));
    }
 
    private ProviderResponse mapToResponse(Provider provider) {
        return ProviderResponse.builder()
                .providerId(provider.getProviderId())
                .userId(provider.getUserId())
                .specialization(provider.getSpecialization())
                .qualification(provider.getQualification())
                .experienceYears(provider.getExperienceYears())
                .bio(provider.getBio())
                .clinicName(provider.getClinicName())
                .clinicAddress(provider.getClinicAddress())
                .avgRating(provider.getAvgRating())
                .isVerified(provider.getIsVerified())
                .isAvailable(provider.getIsAvailable())
                .createdAt(provider.getCreatedAt())
                .build();
    }
}