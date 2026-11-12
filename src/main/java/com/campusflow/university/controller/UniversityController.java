package com.campusflow.university.controller;

import com.campusflow.common.web.CurrentUserId;
import com.campusflow.university.dto.UniversityRequest;
import com.campusflow.university.dto.UniversityResponse;
import com.campusflow.university.service.UniversityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** GET is public; /api/admin/** is limited to ROLE_ADMIN in SecurityConfig. */
@Tag(name = "Universities", description = "Universities recognised from students' email domains")
@RestController
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;

    @Operation(summary = "All known universities (for filters such as items?universityId=)")
    @GetMapping("/api/universities")
    public List<UniversityResponse> findAll() {
        return universityService.findAll();
    }

    @Operation(summary = "Add a university; students who already registered with its domain are linked")
    @PostMapping("/api/admin/universities")
    @ResponseStatus(HttpStatus.CREATED)
    public UniversityResponse create(@CurrentUserId Long adminId, @Valid @RequestBody UniversityRequest request) {
        return universityService.create(adminId, request);
    }

    @Operation(summary = "Update a university")
    @PutMapping("/api/admin/universities/{id}")
    public UniversityResponse update(@CurrentUserId Long adminId, @PathVariable Long id,
            @Valid @RequestBody UniversityRequest request) {
        return universityService.update(adminId, id, request);
    }

    @Operation(summary = "Delete a university (users keep their account, without a university)")
    @DeleteMapping("/api/admin/universities/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long adminId, @PathVariable Long id) {
        universityService.delete(adminId, id);
    }
}
