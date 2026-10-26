package com.campusflow.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusflow.common.exception.ApiException;
import com.campusflow.exchange.entity.ExchangeRequest;
import com.campusflow.exchange.entity.ExchangeStatus;
import com.campusflow.exchange.repository.ExchangeRequestRepository;
import com.campusflow.item.entity.Item;
import com.campusflow.review.dto.ReviewRequest;
import com.campusflow.review.dto.ReviewResponse;
import com.campusflow.review.entity.Review;
import com.campusflow.review.repository.RatingStats;
import com.campusflow.review.repository.ReviewRepository;
import com.campusflow.user.entity.User;
import com.campusflow.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ExchangeRequestRepository exchangeRequestRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ReviewService service;

    private User owner;
    private User requester;
    private ExchangeRequest exchange;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).fullName("Owner").build();
        requester = User.builder().id(2L).fullName("Req").build();
        Item item = Item.builder().id(10L).owner(owner).title("Giai tich 1").build();
        exchange = ExchangeRequest.builder().id(100L).item(item).requester(requester)
                .status(ExchangeStatus.COMPLETED).build();
        lenient().when(exchangeRequestRepository.findWithDetailsById(100L)).thenReturn(Optional.of(exchange));
        lenient().when(userRepository.getReferenceById(2L)).thenReturn(requester);
        lenient().when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void requesterReviewsOwnerAndReputationIsRecomputed() {
        when(reviewRepository.ratingStats(1L)).thenReturn(new RatingStats(4.666666, 3L));

        ReviewResponse response = service.create(2L, 100L, new ReviewRequest(5, " Great "));

        ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(saved.capture());
        assertThat(saved.getValue().getReviewee()).isSameAs(owner);
        assertThat(response.comment()).isEqualTo("Great");
        assertThat(owner.getReputationScore()).isEqualByComparingTo("4.67");
        assertThat(owner.getReviewCount()).isEqualTo(3);
    }

    @Test
    void cannotReviewUnfinishedExchange() {
        exchange.setStatus(ExchangeStatus.ACCEPTED);

        assertThatThrownBy(() -> service.create(2L, 100L, new ReviewRequest(5, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void cannotReviewTwice() {
        when(reviewRepository.existsByExchangeRequestIdAndReviewerId(100L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(2L, 100L, new ReviewRequest(4, null)))
                .hasMessageContaining("already reviewed");
    }

    @Test
    void outsiderCannotReview() {
        assertThatThrownBy(() -> service.create(3L, 100L, new ReviewRequest(1, "spam")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
