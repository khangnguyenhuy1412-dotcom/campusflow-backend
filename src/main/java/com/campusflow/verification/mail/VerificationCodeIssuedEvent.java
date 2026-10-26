package com.campusflow.verification.mail;

import com.campusflow.verification.entity.VerificationPurpose;
import java.time.Duration;

public record VerificationCodeIssuedEvent(
        String email, String fullName, String code, VerificationPurpose purpose, Duration validFor) {
}
