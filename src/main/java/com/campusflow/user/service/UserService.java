package com.campusflow.user.service;

import com.campusflow.common.exception.ApiException;
import com.campusflow.user.dto.UserResponse;
import com.campusflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
