package com.bookhub.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    // Thêm phương thức tìm kiếm theo ID
    Optional<User> findByIdUser(Integer idUser);
}