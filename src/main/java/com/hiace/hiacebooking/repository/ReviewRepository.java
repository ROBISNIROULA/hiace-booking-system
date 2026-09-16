package com.hiace.hiacebooking.repository;

import com.hiace.hiacebooking.model.Hiace;
import com.hiace.hiacebooking.model.Review;
import com.hiace.hiacebooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository
        extends JpaRepository<Review, Long> {

    List<Review> findByHiace(Hiace hiace);

    List<Review> findByUser(User user);

    Optional<Review> findByUserAndBooking(
        User user,
        com.hiace.hiacebooking.model.Booking booking);

    @Query("SELECT AVG(r.rating) FROM Review r " +
           "WHERE r.hiace = :hiace")
    Double findAverageRatingByHiace(Hiace hiace);

    @Query("SELECT COUNT(r) FROM Review r " +
           "WHERE r.hiace = :hiace")
    Long countByHiace(Hiace hiace);
}