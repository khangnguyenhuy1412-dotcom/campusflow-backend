package com.campusflow.user.controller;

import com.campusflow.auth.dto.ChangePasswordRequest;
import com.campusflow.auth.service.PasswordService;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.web.CurrentUserId;
import com.campusflow.item.dto.ItemSummaryResponse;
import com.campusflow.item.entity.ItemStatus;
import com.campusflow.item.service.ItemService;
import com.campusflow.review.dto.ReviewResponse;
import com.campusflow.review.service.ReviewService;
import com.campusflow.user.dto.PublicProfileResponse;
import com.campusflow.user.dto.UpdateProfileRequest;
import com.campusflow.user.dto.UserResponse;
import com.campusflow.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * /api/users/me/** is the logged-in user's own account (login required).
 * /api/users/{id}/** is the public profile of any user (no login, no email or phone).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordService passwordService;
    private final ItemService itemService;
    private final ReviewService reviewService;

    @GetMapping("/me")
    public UserResponse me(@CurrentUserId Long userId) {
        return userService.getById(userId);
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@CurrentUserId Long userId, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId, request);
    }

    @PostMapping(path = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse updateAvatar(@CurrentUserId Long userId, @RequestParam("file") MultipartFile file) {
        return userService.updateAvatar(userId, file);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@CurrentUserId Long userId, @Valid @RequestBody ChangePasswordRequest request) {
        passwordService.changePassword(userId, request);
    }

    @GetMapping("/me/items")
    public PageResponse<ItemSummaryResponse> myItems(@CurrentUserId Long userId,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return itemService.listMine(userId, status, page, size);
    }

    @GetMapping("/{id}")
    public PublicProfileResponse profile(@PathVariable Long id) {
        return userService.getPublicProfile(id);
    }

    @GetMapping("/{id}/items")
    public PageResponse<ItemSummaryResponse> items(@PathVariable Long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return itemService.listPublicByOwner(id, page, size);
    }

    @GetMapping("/{id}/reviews")
    public PageResponse<ReviewResponse> reviews(@PathVariable Long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return reviewService.listForUser(id, page, size);
    }
}
