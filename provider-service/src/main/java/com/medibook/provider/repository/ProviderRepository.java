package com.medibook.provider.repository;
 
import com.medibook.provider.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.Optional;
 
@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {
 
    Optional<Provider> findByUserId(Long userId);
 
    List<Provider> findBySpecializationIgnoreCase(String specialization);
 
    List<Provider> findByIsVerified(Boolean isVerified);
 
    List<Provider> findByIsAvailable(Boolean isAvailable);
 
    int countBySpecialization(String specialization);
 
    // ── Search by name or specialization ──────────────────────────────────
 
    @Query("SELECT p FROM Provider p WHERE " +
           "LOWER(p.specialization) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.clinicName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Provider> searchByNameOrSpecialization(@Param("query") String query);
 
    // ── Find by clinic address / location ─────────────────────────────────
 
    @Query("SELECT p FROM Provider p WHERE " +
           "LOWER(p.clinicAddress) LIKE LOWER(CONCAT('%', :location, '%'))")
    List<Provider> findByClinicAddressContaining(@Param("location") String location);
 
    // ── Advanced filter (used by patients to narrow search results) ────────
 
    @Query("SELECT p FROM Provider p WHERE " +
           "p.isVerified = true AND p.isAvailable = true AND " +
           "(:specialization IS NULL OR LOWER(p.specialization) = LOWER(:specialization)) AND " +
           "(:location IS NULL OR LOWER(p.clinicAddress) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:minRating IS NULL OR p.avgRating >= :minRating)")
    List<Provider> filterProviders(
            @Param("specialization") String specialization,
            @Param("location") String location,
            @Param("minRating") Double minRating);
 
    // ── Find verified providers by specialization ──────────────────────────
 
    @Query("SELECT p FROM Provider p WHERE " +
           "p.isVerified = true AND " +
           "LOWER(p.specialization) = LOWER(:specialization)")
    List<Provider> findVerifiedBySpecialization(@Param("specialization") String specialization);
 
    boolean existsByUserId(Long userId);
}