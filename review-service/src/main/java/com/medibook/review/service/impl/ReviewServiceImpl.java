package com.medibook.review.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.medibook.review.client.AppointmentClient;
import com.medibook.review.client.ProviderClient;
import com.medibook.review.dto.request.AppointmentDto;
import com.medibook.review.dto.request.ReviewRequest;
import com.medibook.review.entity.Review;
import com.medibook.review.exception.BadRequestException;
import com.medibook.review.exception.DuplicateResourceException;
import com.medibook.review.exception.ResourceNotFoundException;
import com.medibook.review.repository.ReviewRepository;
import com.medibook.review.service.ReviewService;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AppointmentClient appointmentClient;

    @Autowired
    private ProviderClient providerClient;

    @Override
    public Review submitReview(ReviewRequest request) {

        // Feign call → appointment-service GET /appointments/{id}
        AppointmentDto appointment = appointmentClient.getById(request.getAppointmentId());

        if(!appointment.getStatus().equals("COMPLETED")) {
            throw new BadRequestException(
                "You can only review after appointment is completed. Status: " + appointment.getStatus());
        }

        if(reviewRepository.findByAppointmentId(request.getAppointmentId()).isPresent()) {
            throw new DuplicateResourceException(
                "Review already exists for appointment: " + request.getAppointmentId());
        }

        if(request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5 stars.");
        }

        Review review = Review.builder()
                .appointmentId(request.getAppointmentId())
                .patientId(request.getPatientId())
                .providerId(request.getProviderId())
                .rating(request.getRating())
                .comment(request.getComment())
                .isAnonymous(request.isAnonymous())
                .build();

        Review saved = reviewRepository.save(review);

        // Feign call → provider-service PUT /providers/{id}/rating
        updateDoctorRating(request.getProviderId());

        return saved;
    }

    @Override
    public List<Review> getReviewsByProvider(int providerId) {
        return reviewRepository.findByProviderIdOrderByCreatedAtDesc(providerId);
    }

    @Override
    public List<Review> getReviewsByPatient(int patientId) {
        return reviewRepository.findByPatientId(patientId);
    }

    @Override
    public Review getReviewById(int reviewId) {
        return reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
    }

    @Override
    public Review updateReview(int reviewId, ReviewRequest request) {
        Review existing = getReviewById(reviewId);

        if(request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5 stars.");
        }

        existing.setRating(request.getRating());
        existing.setComment(request.getComment());
        existing.setAnonymous(request.isAnonymous());

        Review saved = reviewRepository.save(existing);
        updateDoctorRating(existing.getProviderId());
        return saved;
    }

    @Override
    public void deleteReview(int reviewId) {
        Review review = getReviewById(reviewId);
        int providerId = review.getProviderId();
        reviewRepository.deleteById(reviewId);
        updateDoctorRating(providerId);
    }

    @Override
    public double getAverageRating(int providerId) {
        Double avg = reviewRepository.calculateAverageRatingByProviderId(providerId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Override
    public long getReviewCount(int providerId) {
        return reviewRepository.countByProviderId(providerId);
    }

    /**
     * Recalculate avgRating from all reviews then update via Feign.
     * Replaces: providerService.updateRating(providerId, newAvg)
     */
    private void updateDoctorRating(int providerId) {
        double newAvg = getAverageRating(providerId);
        // Feign call → provider-service PUT /providers/{id}/rating?avgRating=4.5
        providerClient.updateRating(providerId, newAvg);
    }
}
