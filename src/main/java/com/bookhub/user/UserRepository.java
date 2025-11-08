package com.bookhub.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    // Phương thức tìm kiếm theo ID (đã có)
    Optional<User> findByIdUser(Integer idUser);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.addresses a WHERE u.idUser = ?1")
    Optional<User> findByIdWithAddresses(Integer idUser);


}