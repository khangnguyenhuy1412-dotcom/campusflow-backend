package com.campusflow.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusflow.exchange.entity.ExchangeRequest;
import com.campusflow.exchange.entity.ExchangeStatus;
import com.campusflow.exchange.event.ExchangeEvent;
import com.campusflow.exchange.event.ExchangeEventType;
import com.campusflow.item.entity.Item;
import com.campusflow.item.entity.ItemType;
import com.campusflow.notification.entity.NotificationType;
import com.campusflow.review.entity.Review;
import com.campusflow.user.entity.User;
import org.junit.jupiter.api.Test;

class NotificationMessagesTest {

    private final User owner = User.builder().id(1L).fullName("An").build();
    private final User requester = User.builder().id(2L).fullName("Bình").build();
    private final Item item = Item.builder().id(10L).owner(owner).title("Nồi cơm điện").itemType(ItemType.GIVEAWAY).build();
    private final ExchangeRequest request = ExchangeRequest.builder()
            .id(100L).item(item).requester(requester).status(ExchangeStatus.PENDING).build();

    private NotificationDraft draft(ExchangeEventType type, Long actorId) {
        return NotificationMessages.forExchange(new ExchangeEvent(request, type, actorId));
    }

    @Test
    void newRequestGoesToOwnerWithVerbForListingType() {
        NotificationDraft draft = draft(ExchangeEventType.REQUESTED, 2L);

        assertThat(draft.recipient()).isSameAs(owner);
        assertThat(draft.actor()).isSameAs(requester);
        assertThat(draft.type()).isEqualTo(NotificationType.EXCHANGE_REQUESTED);
        assertThat(draft.message()).isEqualTo("Bình muốn nhận “Nồi cơm điện”.");
        assertThat(draft.exchangeRequest()).isSameAs(request);
    }

    @Test
    void acceptRejectAndCompleteGoToRequester() {
        assertThat(draft(ExchangeEventType.ACCEPTED, 1L).recipient()).isSameAs(requester);
        assertThat(draft(ExchangeEventType.REJECTED, 1L).recipient()).isSameAs(requester);
        assertThat(draft(ExchangeEventType.COMPLETED, 1L).message()).contains("đánh giá");
    }

    @Test
    void completionByEitherSideNotifiesTheOther() {
        assertThat(draft(ExchangeEventType.COMPLETED, 1L).recipient()).as("owner completed").isSameAs(requester);
        assertThat(draft(ExchangeEventType.COMPLETED, 2L).recipient()).as("receiver confirmed handover").isSameAs(owner);
    }

    @Test
    void automaticRejectionHasNoActor() {
        NotificationDraft draft = draft(ExchangeEventType.AUTO_REJECTED, null);

        assertThat(draft.recipient()).isSameAs(requester);
        assertThat(draft.actor()).isNull();
        assertThat(draft.type()).isEqualTo(NotificationType.EXCHANGE_REJECTED);
    }

    @Test
    void cancellationGoesToTheOtherParty() {
        assertThat(draft(ExchangeEventType.CANCELLED, 2L).recipient()).as("requester cancelled").isSameAs(owner);
        assertThat(draft(ExchangeEventType.CANCELLED, 1L).recipient()).as("owner cancelled").isSameAs(requester);
        assertThat(draft(ExchangeEventType.AUTO_CANCELLED, null).recipient()).isSameAs(owner);
    }

    @Test
    void reviewGoesToReviewee() {
        Review review = Review.builder().exchangeRequest(request).reviewer(requester).reviewee(owner).rating(5).build();

        NotificationDraft draft = NotificationMessages.forReview(review);

        assertThat(draft.recipient()).isSameAs(owner);
        assertThat(draft.title()).isEqualTo("Bạn nhận được đánh giá 5★");
    }

    @Test
    void longTitlesAreShortened() {
        String quoted = NotificationMessages.quote("x".repeat(200));

        assertThat(quoted).hasSize(62).endsWith("…”");
    }
}
