package com.campusflow.chat.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusflow.user.entity.User;
import org.junit.jupiter.api.Test;

class ConversationTest {

    private final User buyer = User.builder().id(1L).build();
    private final User seller = User.builder().id(2L).build();
    private final Conversation conversation = Conversation.builder().buyer(buyer).seller(seller).build();

    @Test
    void readPointerOnlyMovesForward() {
        conversation.markReadUpTo(1L, 10L);
        conversation.markReadUpTo(1L, 7L); // an older "read" request arriving late must not undo it

        assertThat(conversation.getBuyerLastReadMessageId()).isEqualTo(10L);
        assertThat(conversation.getSellerLastReadMessageId()).isNull();
    }

    @Test
    void eachParticipantHasTheirOwnPointer() {
        conversation.markReadUpTo(2L, 5L);

        assertThat(conversation.getSellerLastReadMessageId()).isEqualTo(5L);
        assertThat(conversation.getBuyerLastReadMessageId()).isNull();
    }

    @Test
    void participantsAndCounterparts() {
        assertThat(conversation.isParticipant(1L)).isTrue();
        assertThat(conversation.isParticipant(3L)).isFalse();
        assertThat(conversation.counterpartOf(1L)).isSameAs(seller);
        assertThat(conversation.counterpartOf(2L)).isSameAs(buyer);
    }
}
