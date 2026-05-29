package com.site.toffCo.infra.utils;

import com.site.toffCo.infra.exception.user.UserNotFound;
import com.site.toffCo.module.user.entity.User;
import com.site.toffCo.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUtil {

    private final UserRepository userRepository;

    public User getUserLogado(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (String) authentication.getPrincipal();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFound("User not found"));
    }
}
