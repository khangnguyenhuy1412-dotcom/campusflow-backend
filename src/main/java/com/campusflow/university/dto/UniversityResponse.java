package com.campusflow.university.dto;

import com.campusflow.university.entity.University;

public record UniversityResponse(Long id, String name, String shortName, String emailDomain) {

    public static UniversityResponse from(University university) {
        return new UniversityResponse(
                university.getId(), university.getName(), university.getShortName(), university.getEmailDomain());
    }
}
