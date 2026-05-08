package com.medibook.review.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medibook.review.client.AppointmentClient;
import com.medibook.review.client.ProviderClient;
import com.medibook.review.dto.request.AppointmentDto;
import com.medibook.review.dto.request.ReviewRequest;
import com.medibook.review.entity.Review;
import com.medibook.review.exception.BadRequestException;
import com.medibook.review.exception.DuplicateResourceException;
import com.medibook.review.exception.ResourceNotFoundException;
import com.medibook.review.repository.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private AppointmentClient appointmentClient;
    @Mock private ProviderClient providerClient;
    @InjectMocks private ReviewServiceImpl reviewService;

    private Review review;
    private ReviewRequest request;
    private AppointmentDto completed;

    @BeforeEach
    void setUp() {
        review = Review.builder()
                .reviewId(1)
                .appointmentId(2)
                .patientId(3)
                .providerId(4)
                .rating(5)
                .comment("Excellent")
                .isAnonymous(false)
                .build();
        request = new ReviewRequest();
        request.setAppointmentId(2);
        request.setPatientId(3);
        request.setProviderId(4);
        request.setRating(5);
        request.setComment("Excellent");
        completed = new AppointmentDto();
        completed.setStatus("COMPLETED");
    }

    @Test
    void submitReviewSavesAndUpdatesProviderRating() {
        when(appointmentClient.getById(2)).thenReturn(completed);
        when(reviewRepository.findByAppointmentId(2)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewRepository.calculateAverageRatingByProviderId(4)).thenReturn(4.6);

        Review saved = reviewService.submitReview(request);

        assertThat(saved.getRating()).isEqualTo(5);
        verify(providerClient).updateRating(4, 4.6);
    }

    @Test
    void submitReviewValidationsThrow() {
        AppointmentDto scheduled = new AppointmentDto();
        scheduled.setStatus("SCHEDULED");
        when(appointmentClient.getById(2)).thenReturn(scheduled);
        assertThatThrownBy(() -> reviewService.submitReview(request)).isInstanceOf(BadRequestException.class);

        when(appointmentClient.getById(2)).thenReturn(completed);
        when(reviewRepository.findByAppointmentId(2)).thenReturn(Optional.of(review));
        assertThatThrownBy(() -> reviewService.submitReview(request)).isInstanceOf(DuplicateResourceException.class);

        when(reviewRepository.findByAppointmentId(2)).thenReturn(Optional.empty());
        request.setRating(6);
        assertThatThrownBy(() -> reviewService.submitReview(request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void readAverageAndCountMethodsUseRepository() {
        when(reviewRepository.findByProviderIdOrderByCreatedAtDesc(4)).thenReturn(List.of(review));
        when(reviewRepository.findByPatientId(3)).thenReturn(List.of(review));
        when(reviewRepository.findByReviewId(1)).thenReturn(Optional.of(review));
        when(reviewRepository.calculateAverageRatingByProviderId(4)).thenReturn(4.66);
        when(reviewRepository.countByProviderId(4)).thenReturn(9L);

        assertThat(reviewService.getReviewsByProvider(4)).containsExactly(review);
        assertThat(reviewService.getReviewsByPatient(3)).containsExactly(review);
        assertThat(reviewService.getReviewById(1)).isSameAs(review);
        assertThat(reviewService.getAverageRating(4)).isEqualTo(4.7);
        assertThat(reviewService.getReviewCount(4)).isEqualTo(9);
    }

    @Test
    void updateAndDeleteReviewRecalculateRating() {
        request.setRating(4);
        request.setAnonymous(true);
        when(reviewRepository.findByReviewId(1)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewRepository.calculateAverageRatingByProviderId(4)).thenReturn(null);

        Review updated = reviewService.updateReview(1, request);
        reviewService.deleteReview(1);

        assertThat(updated.getRating()).isEqualTo(4);
        assertThat(updated.isAnonymous()).isTrue();
        verify(providerClient, times(2)).updateRating(4, 0.0);
        verify(reviewRepository).deleteById(1);
    }

    @Test
    void missingReviewThrows() {
        when(reviewRepository.findByReviewId(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewById(404))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
