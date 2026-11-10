package com.campusflow.security;

import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Answers "is this account still allowed in?" for every authenticated request. Access tokens stay
 * valid for 15 minutes, so without this a banned user could keep using the API until expiry.
 * Results are cached briefly to avoid a database query per request; banning evicts the entry,
 * so a ban takes effect immediately on this instance (within the TTL on others).
 */
@Service
public class AccountStatusService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final UserRepository userRepository;
    private final Cache<Long, Boolean> activeByUserId = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterWrite(CACHE_TTL)
            .build();

    public AccountStatusService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isActive(Long userId) {
        return activeByUserId.get(userId,
                id -> userRepository.findById(id).map(User::isActive).orElse(false));
    }

    /**
     * Call when an account's active flag changes. The entry is dropped after the transaction
     * commits; dropping it earlier could let a concurrent request cache the old value again.
     */
    public void evictAfterCommit(Long userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            activeByUserId.invalidate(userId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                activeByUserId.invalidate(userId);
            }
        });
    }
}
