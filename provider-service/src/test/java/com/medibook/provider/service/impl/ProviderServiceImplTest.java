package com.medibook.provider.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medibook.provider.dto.request.ProviderRequest;
import com.medibook.provider.entity.Provider;
import com.medibook.provider.exception.BadRequestException;
import com.medibook.provider.exception.DuplicateResourceException;
import com.medibook.provider.exception.ResourceNotFoundException;
import com.medibook.provider.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
class ProviderServiceImplTest {

    @Mock private ProviderRepository providerRepository;
    @InjectMocks private ProviderServiceImpl providerService;

    private Provider provider;

    @BeforeEach
    void setUp() {
        provider = Provider.builder()
                .providerId(10)
                .userId(5)
                .specialization("Cardiology")
                .qualification("MBBS")
                .experienceYears(8)
                .bio("Heart specialist")
                .clinicName("Care Clinic")
                .clinicAddress("Pune")
                .consultationFee(800.0)
                .avgRating(4.0)
                .verified(true)
                .isAvailable(true)
                .build();
    }

    @Test
    void registerProviderDefaultsAndRejectsDuplicateUserId() {
        ProviderRequest request = request();
        request.setConsultationFee(0);
        when(providerRepository.findByUserId(5)).thenReturn(Optional.empty());
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Provider saved = providerService.registerProvider(request);

        assertThat(saved.getConsultationFee()).isEqualTo(500.0);
        assertThat(saved.isVerified()).isFalse();
        assertThat(saved.isAvailable()).isTrue();

        when(providerRepository.findByUserId(5)).thenReturn(Optional.of(provider));
        assertThatThrownBy(() -> providerService.registerProvider(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void lookupSearchAndFilterMethodsUseRepository() {
        Provider unverified = Provider.builder().providerId(11).verified(false).build();
        when(providerRepository.findById(10)).thenReturn(Optional.of(provider));
        when(providerRepository.findByUserId(5)).thenReturn(Optional.of(provider));
        when(providerRepository.findBySpecialization("Cardiology")).thenReturn(List.of(provider, unverified));
        when(providerRepository.searchByNameOrSpecialization("cardio")).thenReturn(List.of(provider));
        when(providerRepository.findByVerifiedAndIsAvailable(true, true)).thenReturn(List.of(provider));
        when(providerRepository.findAll()).thenReturn(List.of(provider, unverified));

        assertThat(providerService.getProviderById(10)).isSameAs(provider);
        assertThat(providerService.getProviderByUserId(5)).isSameAs(provider);
        assertThat(providerService.getBySpecialization("Cardiology")).containsExactly(provider);
        assertThat(providerService.searchProviders("cardio")).containsExactly(provider);
        assertThat(providerService.getVerifiedAndAvailableProviders()).containsExactly(provider);
        assertThat(providerService.getAllProviders()).hasSize(2);
    }

    @Test
    void validationsThrowBadRequestOrNotFound() {
        when(providerRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> providerService.getProviderById(404))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> providerService.getBySpecialization(" "))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> providerService.searchProviders(null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> providerService.updateRating(10, 5.5))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> providerService.updateFee(10, -1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateVerifyAvailabilityFeeRatingAndDeleteMutateProvider() {
        ProviderRequest request = request();
        request.setSpecialization("Dermatology");
        request.setConsultationFee(900.0);
        when(providerRepository.findById(10)).thenReturn(Optional.of(provider));
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Provider updated = providerService.updateProvider(10, request);
        Provider verified = providerService.verifyProvider(10);
        providerService.setAvailability(10, false);
        Provider feeUpdated = providerService.updateFee(10, 700.0);
        providerService.updateRating(10, 4.5);

        assertThat(updated.getSpecialization()).isEqualTo("Dermatology");
        assertThat(verified.isVerified()).isTrue();
        assertThat(feeUpdated.getConsultationFee()).isEqualTo(700.0);
        clearInvocations(providerRepository);

        providerService.updateRating(10, 4.5);
        verify(providerRepository).save(argThat(p -> p.getAvgRating() == 4.5));

        providerService.deleteProvider(10);
        verify(providerRepository).deleteById(10);
    }

    private ProviderRequest request() {
        ProviderRequest request = new ProviderRequest();
        request.setUserId(5);
        request.setSpecialization("Cardiology");
        request.setQualification("MBBS");
        request.setExperienceYears(8);
        request.setBio("Heart specialist");
        request.setClinicName("Care Clinic");
        request.setClinicAddress("Pune");
        request.setConsultationFee(800.0);
        return request;
    }
}
