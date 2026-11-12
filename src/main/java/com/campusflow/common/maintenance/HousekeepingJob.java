package com.campusflow.common.maintenance;

import com.campusflow.auth.repository.RefreshTokenRepository;
import com.campusflow.notification.repository.NotificationRepository;
import com.campusflow.verification.repository.VerificationCodeRepository;
import com.campusflow.wanted.repository.WantedPostRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Nightly cleanup so tables that only ever grow stay small. */
@Slf4j
@Component
public class HousekeepingJob {

    /** Expired tokens/codes are kept one more day, which helps when investigating issues. */
    private static final Duration EXPIRED_GRACE = Duration.ofDays(1);

    private final NotificationRepository notificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final WantedPostRepository wantedPostRepository;
    private final Duration notificationRetention;

    public HousekeepingJob(NotificationRepository notificationRepository,
            RefreshTokenRepository refreshTokenRepository,
            VerificationCodeRepository verificationCodeRepository,
            WantedPostRepository wantedPostRepository,
            @Value("${app.housekeeping.notification-retention}") Duration notificationRetention) {
        this.notificationRepository = notificationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.wantedPostRepository = wantedPostRepository;
        this.notificationRetention = notificationRetention;
    }

    @Scheduled(cron = "${app.housekeeping.cron}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void run() {
        Instant now = Instant.now();
        int notifications = notificationRepository.deleteReadBefore(now.minus(notificationRetention));
        int tokens = refreshTokenRepository.deleteExpiredBefore(now.minus(EXPIRED_GRACE));
        int codes = verificationCodeRepository.deleteExpiredBefore(now.minus(EXPIRED_GRACE));
        int wantedPosts = wantedPostRepository.closeExpired(now);
        log.info("Housekeeping removed {} read notifications, {} refresh tokens, {} verification codes; "
                + "closed {} expired wanted posts", notifications, tokens, codes, wantedPosts);
    }
}
