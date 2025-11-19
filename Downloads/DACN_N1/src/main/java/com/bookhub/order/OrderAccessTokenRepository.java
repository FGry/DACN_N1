package com.bookhub.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderAccessTokenRepository extends JpaRepository<OrderAccessToken, Integer> {
    Optional<OrderAccessToken> findByAccessToken(String accessToken);
}