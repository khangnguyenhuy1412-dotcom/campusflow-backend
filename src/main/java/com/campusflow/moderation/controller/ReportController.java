package com.campusflow.moderation.controller;

import com.campusflow.common.web.CurrentUserId;
import com.campusflow.moderation.dto.CreateReportRequest;
import com.campusflow.moderation.dto.MyReportResponse;
import com.campusflow.moderation.service.ReportService;
import com.campusflow.ratelimit.RateLimit;
import com.campusflow.ratelimit.RateLimitPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports", description = "Report a listing or a user to the moderators")
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Report a listing or a user (verified email required)")
    @RateLimit(policy = RateLimitPolicy.REPORT, key = RateLimit.Key.USER)
    @PostMapping("/api/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public MyReportResponse create(@CurrentUserId Long userId, @Valid @RequestBody CreateReportRequest request) {
        return reportService.create(userId, request);
    }
}
