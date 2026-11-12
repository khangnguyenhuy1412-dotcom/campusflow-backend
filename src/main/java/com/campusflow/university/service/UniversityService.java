package com.campusflow.university.service;

import com.campusflow.audit.entity.AuditAction;
import com.campusflow.audit.service.AuditService;
import com.campusflow.common.exception.ApiException;
import com.campusflow.university.dto.UniversityRequest;
import com.campusflow.university.dto.UniversityResponse;
import com.campusflow.university.entity.University;
import com.campusflow.university.repository.UniversityRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UniversityService {

    private final UniversityRepository universityRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<UniversityResponse> findAll() {
        return universityRepository.findAll(Sort.by("shortName")).stream().map(UniversityResponse::from).toList();
    }

    /**
     * Finds the university of an email address. The longest matching domain wins, so a specific
     * domain (e.g. a faculty's own domain) is preferred over its parent.
     */
    @Transactional(readOnly = true)
    public Optional<University> resolve(String email) {
        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase(Locale.ROOT);
        return universityRepository.findAll().stream()
                .filter(university -> university.matchesDomain(domain))
                .max(Comparator.comparingInt(university -> university.getEmailDomain().length()));
    }

    /** Adds a university and links the students who already registered with its domain. */
    @Transactional
    public UniversityResponse create(Long adminId, UniversityRequest request) {
        if (universityRepository.existsByEmailDomain(request.emailDomain())) {
            throw ApiException.conflict("A university with this email domain already exists");
        }
        University university = universityRepository.save(University.builder()
                .name(request.name().strip())
                .shortName(request.shortName().strip())
                .emailDomain(request.emailDomain())
                .build());
        int linked = universityRepository.assignUsersByDomain(university.getId(), university.getName(),
                university.getEmailDomain());
        auditService.record(adminId, AuditAction.UNIVERSITY_CREATED, "UNIVERSITY", university.getId(),
                university.getEmailDomain() + ", linked " + linked + " existing user(s)");
        return UniversityResponse.from(university);
    }

    @Transactional
    public UniversityResponse update(Long adminId, Long id, UniversityRequest request) {
        University university = universityRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("University not found"));
        if (universityRepository.existsByEmailDomainAndIdNot(request.emailDomain(), id)) {
            throw ApiException.conflict("A university with this email domain already exists");
        }
        university.setName(request.name().strip());
        university.setShortName(request.shortName().strip());
        university.setEmailDomain(request.emailDomain());
        universityRepository.renameForUsers(id, university.getName());
        auditService.record(adminId, AuditAction.UNIVERSITY_UPDATED, "UNIVERSITY", id, university.getEmailDomain());
        return UniversityResponse.from(university);
    }

    @Transactional
    public void delete(Long adminId, Long id) {
        University university = universityRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("University not found"));
        universityRepository.clearForUsers(id);
        universityRepository.delete(university);
        auditService.record(adminId, AuditAction.UNIVERSITY_DELETED, "UNIVERSITY", id, university.getEmailDomain());
    }
}
