package com.campusflow.user.service;

import com.campusflow.common.exception.ApiException;
import com.campusflow.storage.FileStorage;
import com.campusflow.storage.ImageProcessor;
import com.campusflow.storage.ImageProcessor.ProcessedImage;
import com.campusflow.storage.StoredFileCleanup;
import com.campusflow.user.dto.PublicProfileResponse;
import com.campusflow.user.dto.UpdateProfileRequest;
import com.campusflow.user.dto.UserResponse;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int AVATAR_SIZE = 400;

    private final UserRepository userRepository;
    private final ImageProcessor imageProcessor;
    private final FileStorage fileStorage;
    private final StoredFileCleanup storedFileCleanup;

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(getActiveUser(id));
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(Long id) {
        return PublicProfileResponse.from(getActiveUser(id));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getActiveUser(userId);
        user.setFullName(request.fullName().trim());
        user.setStudentId(blankToNull(request.studentId()));
        user.setUniversity(blankToNull(request.university()));
        user.setPhoneNumber(blankToNull(request.phoneNumber()));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateAvatar(Long userId, MultipartFile file) {
        User user = getActiveUser(userId);
        ProcessedImage avatar = imageProcessor.processSingle(file, AVATAR_SIZE);
        String url = fileStorage.store("avatars", avatar.content(), avatar.extension());
        storedFileCleanup.deleteOnRollback(List.of(url));

        String oldUrl = user.getAvatarUrl();
        user.setAvatarUrl(url);
        if (oldUrl != null) {
            storedFileCleanup.deleteAfterCommit(List.of(oldUrl));
        }
        return UserResponse.from(user);
    }

    /** Deactivated accounts are treated as not existing. */
    private User getActiveUser(Long id) {
        return userRepository.findById(id)
                .filter(User::isActive)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
