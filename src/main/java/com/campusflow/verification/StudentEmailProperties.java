package com.campusflow.verification;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.student-email")
public record StudentEmailProperties(List<String> allowedDomainSuffixes) {
}
