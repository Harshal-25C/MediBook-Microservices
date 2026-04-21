package com.medibook.provider.repository;
 
import com.medibook.provider.entity.Provider;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Integer> {

    // Find provider profile by their userId.
    Optional<Provider> findByUserId(int userId);

    // Find all doctors with a specific specialization.
    List<Provider> findBySpecialization(String specialization);

    // Find all verified doctors on the platform.
    List<Provider> findByVerified(boolean verified);

    // Find all doctors who are currently available.
    List<Provider> findByIsAvailable(boolean isAvailable);

    //Search doctors by name OR specialization.
    @Query(value = "SELECT p.* FROM providers p JOIN users u ON p.user_id = u.user_id " +
           "WHERE LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.specialization) LIKE LOWER(CONCAT('%', :keyword, '%'))",
           nativeQuery = true)
    List<Provider> searchByNameOrSpecialization(@Param("keyword") String keyword);

    // Find doctors by clinic address or location.
    List<Provider> findByClinicAddressContaining(String location);

    // Count how many doctors exist for each specialization.
    long countBySpecialization(String specialization);

    // Find all verified AND available doctors.
    List<Provider> findByVerifiedAndIsAvailable(boolean verified, boolean isAvailable);
    
    
    // Direct update for verification status.
    @Modifying
    @Transactional
    @Query(value = "UPDATE providers SET is_verified = 1 WHERE provider_id = :providerId", 
           nativeQuery = true)
    int forceVerify(@Param("providerId") int providerId);
 
}