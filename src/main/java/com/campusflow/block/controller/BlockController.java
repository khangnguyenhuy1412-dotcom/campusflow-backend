package com.campusflow.block.controller;

import com.campusflow.block.service.BlockService;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.web.CurrentUserId;
import com.campusflow.user.dto.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Blocks", description = "Block users: no chat and no exchange requests in either direction")
@RestController
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @Operation(summary = "Block a user")
    @PostMapping("/api/blocks/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@CurrentUserId Long currentUserId, @PathVariable Long userId) {
        blockService.block(currentUserId, userId);
    }

    @Operation(summary = "Unblock a user")
    @DeleteMapping("/api/blocks/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@CurrentUserId Long currentUserId, @PathVariable Long userId) {
        blockService.unblock(currentUserId, userId);
    }

    @Operation(summary = "Users you blocked")
    @GetMapping("/api/blocks")
    public PageResponse<UserSummaryResponse> list(@CurrentUserId Long currentUserId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return blockService.listBlocked(currentUserId, page, size);
    }
}
