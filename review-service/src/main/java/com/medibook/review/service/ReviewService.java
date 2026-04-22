package com.medibook.review.service;


import java.util.List;

import com.medibook.review.dto.request.ReviewRequest;
import com.medibook.review.entity.Review;

public interface ReviewService {

    Review submitReview(ReviewRequest request);

    List<Review> getReviewsByProvider(int providerId);

    List<Review> getReviewsByPatient(int patientId);

    Review getReviewById(int reviewId);

    Review updateReview(int reviewId, ReviewRequest request);

    void deleteReview(int reviewId);

    double getAverageRating(int providerId);

    long getReviewCount(int providerId);
}