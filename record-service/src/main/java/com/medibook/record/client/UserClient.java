package com.medibook.record.client;

import com.medibook.record.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface UserClient {

    @GetMapping("/auth/profile/{userId}")
    UserDto getUserById(@PathVariable("userId") int userId);
}
