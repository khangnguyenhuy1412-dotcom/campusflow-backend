package com.campusflow.storage;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Keeps files on disk consistent with the database. Files are not part of the transaction,
 * so new files are removed if the transaction rolls back, and removed files are only
 * deleted once the transaction has committed.
 */
@Component
@RequiredArgsConstructor
public class StoredFileCleanup {

    private final FileStorage fileStorage;

    /** Call right after storing new files inside a transaction. */
    public void deleteOnRollback(List<String> urls) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    urls.forEach(fileStorage::delete);
                }
            }
        });
    }

    /** Call when database rows pointing to these files are removed. */
    public void deleteAfterCommit(List<String> urls) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            urls.forEach(fileStorage::delete);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                urls.forEach(fileStorage::delete);
            }
        });
    }
}
