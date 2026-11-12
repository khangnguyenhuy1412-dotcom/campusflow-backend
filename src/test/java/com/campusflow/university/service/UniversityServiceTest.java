package com.campusflow.university.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.campusflow.audit.service.AuditService;
import com.campusflow.university.entity.University;
import com.campusflow.university.repository.UniversityRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UniversityServiceTest {

    @Mock
    private UniversityRepository universityRepository;
    @Mock
    private AuditService auditService;

    private UniversityService service;
    private final University vnu = University.builder().id(1L).emailDomain("vnu.edu.vn").build();
    private final University hus = University.builder().id(2L).emailDomain("hus.vnu.edu.vn").build();
    private final University hcmus = University.builder().id(3L).emailDomain("hcmus.edu.vn").build();

    @BeforeEach
    void setUp() {
        service = new UniversityService(universityRepository, auditService);
        when(universityRepository.findAll()).thenReturn(List.of(vnu, hus, hcmus));
    }

    @Test
    void studentSubdomainsMatchTheirUniversity() {
        assertThat(service.resolve("an@student.hcmus.edu.vn")).contains(hcmus);
        assertThat(service.resolve("AN@HCMUS.EDU.VN")).contains(hcmus);
    }

    @Test
    void longestMatchingDomainWins() {
        assertThat(service.resolve("an@hus.vnu.edu.vn")).contains(hus);
        assertThat(service.resolve("an@vnu.edu.vn")).contains(vnu);
    }

    @Test
    void lookalikeDomainsDoNotMatch() {
        assertThat(service.resolve("an@fakehcmus.edu.vn")).isEmpty();
        assertThat(service.resolve("an@other.edu.vn")).isEmpty();
    }
}
