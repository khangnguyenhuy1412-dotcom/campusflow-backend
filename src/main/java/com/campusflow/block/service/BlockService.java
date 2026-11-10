package com.campusflow.block.service;

import com.campusflow.block.entity.UserBlock;
import com.campusflow.block.repository.UserBlockRepository;
import com.campusflow.common.dto.PageQuery;
import com.campusflow.common.dto.PageResponse;
import com.campusflow.common.exception.ApiException;
import com.campusflow.user.dto.UserSummaryResponse;
import com.campusflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;

    @Transactional
    public void block(Long userId, Long targetId) {
        if (userId.equals(targetId)) {
            throw ApiException.badRequest("You cannot block yourself");
        }
        if (!userRepository.existsById(targetId)) {
            throw ApiException.notFound("User not found");
        }
        UserBlock.Key key = new UserBlock.Key(userId, targetId);
        if (!userBlockRepository.existsById(key)) {
            userBlockRepository.save(UserBlock.builder()
                    .id(key)
                    .blocker(userRepository.getReferenceById(userId))
                    .blocked(userRepository.getReferenceById(targetId))
                    .build());
        }
    }

    @Transactional
    public void unblock(Long userId, Long targetId) {
        userBlockRepository.deleteById(new UserBlock.Key(userId, targetId));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> listBlocked(Long userId, Integer page, Integer size) {
        return PageResponse.of(
                userBlockRepository.findByIdBlockerId(userId, PageQuery.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
                block -> UserSummaryResponse.from(block.getBlocked()));
    }

    /** Throws 403 when either user blocked the other; used before any contact (chat, requests). */
    @Transactional(readOnly = true)
    public void requireNoBlock(Long userId, Long otherUserId) {
        if (userBlockRepository.existsEitherWay(userId, otherUserId)) {
            throw ApiException.forbidden("You cannot contact this user");
        }
    }
}
