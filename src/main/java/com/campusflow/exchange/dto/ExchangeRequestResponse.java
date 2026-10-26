package com.campusflow.exchange.dto;

import com.campusflow.exchange.entity.ExchangeRequest;
import com.campusflow.exchange.entity.ExchangeStatus;
import com.campusflow.item.dto.ItemSummaryResponse;
import com.campusflow.user.dto.UserSummaryResponse;
import java.time.Instant;

/**
 * @param counterpartContact email/phone of the other party; null unless the request
 *                           is ACCEPTED or COMPLETED, to protect users' privacy
 */
public record ExchangeRequestResponse(
        Long id,
        ExchangeStatus status,
        String message,
        ItemSummaryResponse item,
        ItemSummaryResponse offeredItem,
        UserSummaryResponse requester,
        UserSummaryResponse owner,
        ContactInfo counterpartContact,
        Instant createdAt,
        Instant updatedAt) {

    public static ExchangeRequestResponse from(ExchangeRequest request, Long viewerId) {
        boolean shareContact = request.getStatus() == ExchangeStatus.ACCEPTED
                || request.getStatus() == ExchangeStatus.COMPLETED;
        return new ExchangeRequestResponse(
                request.getId(),
                request.getStatus(),
                request.getMessage(),
                ItemSummaryResponse.from(request.getItem()),
                request.getOfferedItem() == null ? null : ItemSummaryResponse.from(request.getOfferedItem()),
                UserSummaryResponse.from(request.getRequester()),
                UserSummaryResponse.from(request.getOwner()),
                shareContact ? ContactInfo.from(request.counterpartOf(viewerId)) : null,
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
