package com.campusflow.verification;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Decides whether an email address belongs to a student, based on its domain. */
@Component
public class StudentEmailPolicy {

    private final List<String> allowedSuffixes;

    public StudentEmailPolicy(StudentEmailProperties properties) {
        this.allowedSuffixes = properties.allowedDomainSuffixes().stream()
                .map(suffix -> suffix.trim().toLowerCase(Locale.ROOT))
                .filter(suffix -> !suffix.isEmpty())
                .toList();
    }

    /** "an@student.hcmus.edu.vn" matches suffix "edu.vn"; "an@notedu.vn" does not. */
    public boolean isStudentEmail(String email) {
        int at = email.lastIndexOf('@');
        if (at < 0) {
            return false;
        }
        String domain = email.substring(at + 1).toLowerCase(Locale.ROOT);
        return allowedSuffixes.stream()
                .anyMatch(suffix -> domain.equals(suffix) || domain.endsWith("." + suffix));
    }

    public List<String> allowedSuffixes() {
        return allowedSuffixes;
    }
}
