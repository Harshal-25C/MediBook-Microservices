package com.medibook.provider.service.impl;
 
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
 
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
    @InjectMocks private ProviderServiceImpl service;
 
    private Provider verifiedProvider;
    private Provider unverifiedProvider;
    private ProviderRequest providerRequest;
 
    @BeforeEach
    void setUp() {
        verifiedProvider = Provider.builder()
                .providerId(1).userId(10)
                .specialization("Cardiology")
                .qualification("MBBS, MD")
                .experienceYears(5)
                .bio("Heart specialist")
                .clinicName("Heart Clinic")
                .clinicAddress("MG Road")
                .consultationFee(600.0)
                .avgRating(4.5)
                .verified(true)
                .isAvailable(true)
                .build();
 
        unverifiedProvider = Provider.builder()
                .providerId(2).userId(11)
                .specialization("Dermatology")
                .verified(false)
                .isAvailable(true)
                .build();
 
        providerRequest = new ProviderRequest();
        providerRequest.setUserId(10);
        providerRequest.setSpecialization("Cardiology");
        providerRequest.setQualification("MBBS");
        providerRequest.setExperienceYears(3);
        providerRequest.setBio("Bio here");
        providerRequest.setClinicName("Clinic");
        providerRequest.setClinicAddress("Street 1");
        providerRequest.setConsultationFee(500.0);
    }
 
    // ─────────────────────────── registerProvider ─────────────────────────
 
    @Test
    void registerProvider_success_savesWithDefaults() {
        when(providerRepository.findByUserId(10)).thenReturn(Optional.empty());
        when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
 
        Provider result = service.registerProvider(providerRequest);
 
        assertThat(result.isVerified()).isFalse();
        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getAvgRating()).isEqualTo(0.0);
        verify(providerRepository).save(any(Provider.class));
    }
 
    @Test
    void registerProvider_defaultFeeApplied_whenZeroFee() {
        providerRequest.setConsultationFee(0);
        when(providerRepository.findByUserId(10)).thenReturn(Optional.empty());
        when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
 
        Provider result = service.registerProvider(providerRequest);
 
        assertThat(result.getConsultationFee()).isEqualTo(500.0); // default
    }
 
    @Test
    void registerProvider_duplicate_throwsDuplicateResourceException() {
        when(providerRepository.findByUserId(10)).thenReturn(Optional.of(verifiedProvider));
 
        assertThatThrownBy(() -> service.registerProvider(providerRequest))
                .isInstanceOf(DuplicateResourceException.class);
        verify(providerRepository, never()).save(any());
    }
 
    // ─────────────────────────── getProviderById ─────────────────────────
 
    @Test
    void getProviderById_found_returnsProvider() {
        when(providerRepository.findById(1)).thenReturn(Optional.of(verifiedProvider));
        assertThat(service.getProviderById(1).getSpecialization()).isEqualTo("Cardiology");
    }
 
    @Test
    void getProviderById_notFound_throwsResourceNotFoundException() {
        when(providerRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getProviderById(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    // ─────────────────────────── getProviderByUserId ─────────────────────
 
    @Test
    void getProviderByUserId_found_returnsProvider() {
        when(providerRepository.findByUserId(10)).thenReturn(Optional.of(verifiedProvider));
        assertThat(service.getProviderByUserId(10).getClinicName()).isEqualTo("Heart Clinic");
    }
 
    @Test
    void getProviderByUserId_notFound_throwsResourceNotFoundException() {
        when(providerRepository.findByUserId(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getProviderByUserId(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    // ─────────────────────────── getBySpecialization ─────────────────────
 
    @Test
    void getBySpecialization_returnsOnlyVerifiedProviders() {
        when(providerRepository.findBySpecialization("Cardiology"))
                .thenReturn(List.of(verifiedProvider, unverifiedProvider));
 
        List<Provider> result = service.getBySpecialization("Cardiology");
 
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isVerified()).isTrue();
    }
 
    @Test
    void getBySpecialization_emptyString_throwsBadRequestException() {
        assertThatThrownBy(() -> service.getBySpecialization(""))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be empty");
    }
 
    @Test
    void getBySpecialization_null_throwsBadRequestException() {
        assertThatThrownBy(() -> service.getBySpecialization(null))
                .isInstanceOf(BadRequestException.class);
    }
 
    // ─────────────────────────── searchProviders ─────────────────────────
 
    @Test
    void searchProviders_success_returnsResults() {
        when(providerRepository.searchByNameOrSpecialization("heart"))
                .thenReturn(List.of(verifiedProvider));
 
        assertThat(service.searchProviders("heart")).hasSize(1);
    }
 
    @Test
    void searchProviders_emptyKeyword_throwsBadRequestException() {
        assertThatThrownBy(() -> service.searchProviders("   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be empty");
    }
 
    @Test
    void searchProviders_nullKeyword_throwsBadRequestException() {
        assertThatThrownBy(() -> service.searchProviders(null))
                .isInstanceOf(BadRequestException.class);
    }
 
    // ─────────────────────────── updateProvider ──────────────────────────
 
    @Test
    void updateProvider_validRequest_updatesFieldsAndSaves() {
        providerRequest.setExperienceYears(8);
        providerRequest.setConsultationFee(800.0);
 
        when(providerRepository.findById(1)).thenReturn(Optional.of(verifiedProvider));
        when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
 
        Provider result = service.updateProvider(1, providerRequest);
 
        assertThat(result.getExperienceYears()).isEqualTo(8);
        assertThat(result.getConsultationFee()).isEqualTo(800.0);
    }
 
    @Test
    void updateProvider_negativeExperience_throwsBadRequestException() {
        providerRequest.setExperienceYears(-1);
        when(providerRepository.findById(1)).thenReturn(Optional.of(verifiedProvider));
 
        assertThatThrownBy(() -> service.updateProvider(1, providerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be negative");
    }
 
    @Test
    void updateProvider_zeroFee_doesNotUpdateFee() {
        providerRequest.setExperienceYears(3);
        providerRequest.setConsultationFee(0);
        when(providerRepository.findById(1)).thenReturn(Optional.of(verifiedProvider));
        when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
 
        Provider result = service.updateProvider(1, providerRequest);
 
        // Fee should remain unchanged when request fee <= 0
        assertThat(result.getConsultationFee()).isEqualTo(600.0);
    }
 
    // ─────────────────────────── verifyProvider ──────────────────────────
 
    @Test
    void verifyProvider_setsVerifiedTrue() {
        when(providerRepository.findById(2)).thenReturn(Optional.of(unverifiedProvider));
        when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
 
        Provider result = service.verifyProvider(2);
 
        assertThat(result.isVerified()).isTrue();
        verify(providerRepository).save(any(Provider.class));
    }
 
    @Test
    void verifyProvider_notFound_throwsResourceNotFoundException() {
        when(providerRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verifyProvider(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    // ─────────────────────────── setAvailability ─────────────────────────
 
    @Test
    void setAvailability_false_updatesAndSaves() {
        when(providerRepository.findById(1)).thenReturn(Optional.of(verifiedProvider));
        when(providerRepository.save(any())).thenReturn(verifiedProvider);
 
        service.setAvailability(1, false);
 
        assertThat(verifiedProvider.isAvailable()).isFalse();
        verify(providerRepository).save(verifiedProvider);
    }
 
    @Test
    void setAvailability_true_updatesAndSaves() {
        verifiedProvider.setAvailable(false);
        when(providerRepository.findById(1)).thenReturn(Optional.of(verifiedProvider));
        when(providerRepository.save(any())).thenReturn(verifiedProvider);
 
        service.setAvailability(1, true);
 
        assertThat(verifiedProvider.isAvailable()).isTrue();
    }
 
    // ─────────────────────────── deleteProvider ──────────────────────────
 
    @Test
    void deleteProvider_exists_deletes() {
        when(providerRepository.findById(1)).thenReturn(Optional.of(verifiedProvider));
        doNothing().when(providerRepository).deleteById(1);
 
        service.deleteProvider(1);
 
        verify(providerRepository).deleteById(1);
    }
 
    @Test
    void deleteProvider_notFound_throwsResourceNotFoundException() {
        when(providerRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteProvider(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    // ─────────────────────────── updateFee ───────────────────────────────
 
    @Test
    void updateFee_valid_updatesAndSaves() {
        when(providerRepository.findById(1)).thenReturn(Optional.of(verifiedProvider));
        when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
 
        Provider result = service.updateFee(1, 700.0);
 
        assertThat(result.getConsultationFee()).isEqualTo(700.0);
    }
 
    @Test
    void updateFee_negativeFee_throwsBadRequestException() {
        assertThatThrownBy(() -> service.updateFee(1, -100.0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be negative");
    }
 
    @Test
    void updateFee_zeroFee_isAllowed() {
        when(providerRepository.findById(1)).thenReturn(Optional.of(verifiedProvider));
        when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
 
        assertThatCode(() -> service.updateFee(1, 0.0)).doesNotThrowAnyException();
    }
 
    // ─────────────────────────── updateRating ────────────────────────────
 
    @Test
    void updateRating_valid_updatesAndSaves() {
        when(providerRepository.findById(1)).thenReturn(Optional.of(verifiedProvider));
        when(providerRepository.save(any())).thenReturn(verifiedProvider);
 
        service.updateRating(1, 4.8);
 
        assertThat(verifiedProvider.getAvgRating()).isEqualTo(4.8);
        verify(providerRepository).save(verifiedProvider);
    }
 
    @Test
    void updateRating_belowZero_throwsBadRequestException() {
        assertThatThrownBy(() -> service.updateRating(1, -0.1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("between 0.0 and 5.0");
    }
 
    @Test
    void updateRating_aboveFive_throwsBadRequestException() {
        assertThatThrownBy(() -> service.updateRating(1, 5.1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("between 0.0 and 5.0");
    }
 
    @Test
    void updateRating_exactBoundary_doesNotThrow() {
        when(providerRepository.findById(1)).thenReturn(Optional.of(verifiedProvider));
        when(providerRepository.save(any())).thenReturn(verifiedProvider);
 
        assertThatCode(() -> service.updateRating(1, 0.0)).doesNotThrowAnyException();
        assertThatCode(() -> service.updateRating(1, 5.0)).doesNotThrowAnyException();
    }
 
    // ─────────────────────────── getAllProviders ──────────────────────────
 
    @Test
    void getAllProviders_returnsAllIncludingUnverified() {
        when(providerRepository.findAll()).thenReturn(List.of(verifiedProvider, unverifiedProvider));
        assertThat(service.getAllProviders()).hasSize(2);
    }
 
    // ─────────────────────────── getVerifiedAndAvailableProviders ─────────
 
    @Test
    void getVerifiedAndAvailableProviders_returnsOnlyBothConditions() {
        when(providerRepository.findByVerifiedAndIsAvailable(true, true))
                .thenReturn(List.of(verifiedProvider));
 
        List<Provider> result = service.getVerifiedAndAvailableProviders();
 
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isVerified()).isTrue();
        assertThat(result.get(0).isAvailable()).isTrue();
    }
}