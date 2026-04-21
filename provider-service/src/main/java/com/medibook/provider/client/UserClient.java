package com.medibook.provider.client;

import com.medibook.provider.dto.response.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client — provider-service calls auth-service.
 * Replaces direct @Autowired UserRepository in ProviderResource.
 * Used to enrich provider profiles with user name, email, phone, profilePicUrl.
 */

@FeignClient(name = "auth-service")
public interface UserClient {

    @GetMapping("/auth/profile/{userId}")
    UserResponseDto getUserById(@PathVariable("userId") int userId);
}
