package com.medibook.provider.service;
 
import java.util.List;

import com.medibook.provider.dto.request.ProviderRequest;
import com.medibook.provider.entity.Provider;

public interface ProviderService {

	Provider registerProvider(ProviderRequest request);
	
	Provider getProviderById(int providerId);

	Provider getProviderByUserId(int userId);

	List<Provider> getBySpecialization(String specialization);

	List<Provider> searchProviders(String keyword);

	Provider updateProvider(int providerId, ProviderRequest request);

	Provider verifyProvider(int providerId);  // Change from void to Provider

	void setAvailability(int providerId, boolean isAvailable);
	
	void deleteProvider(int providerId);
	
	void updateRating(int providerId, double newRating);

	List<Provider> getAllProviders();

	List<Provider> getVerifiedAndAvailableProviders();
}