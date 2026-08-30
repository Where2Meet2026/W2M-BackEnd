package com.w2m.backend.review.repository;

import com.w2m.backend.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByCandidateId(Long candidateId);
}
