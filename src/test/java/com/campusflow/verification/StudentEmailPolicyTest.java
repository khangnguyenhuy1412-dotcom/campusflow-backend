package com.campusflow.verification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class StudentEmailPolicyTest {

    private final StudentEmailPolicy policy = new StudentEmailPolicy(new StudentEmailProperties(List.of(" Edu.VN ")));

    @Test
    void acceptsAnySubdomainOfEduVn() {
        assertThat(policy.isStudentEmail("an@student.hcmus.edu.vn")).isTrue();
        assertThat(policy.isStudentEmail("an@hcmut.edu.vn")).isTrue();
        assertThat(policy.isStudentEmail("AN@HUST.EDU.VN")).isTrue();
    }

    @Test
    void rejectsOtherDomains() {
        assertThat(policy.isStudentEmail("an@gmail.com")).isFalse();
        assertThat(policy.isStudentEmail("an@notedu.vn")).isFalse();
        assertThat(policy.isStudentEmail("an@edu.vn.evil.com")).isFalse();
        assertThat(policy.isStudentEmail("not-an-email")).isFalse();
    }
}
