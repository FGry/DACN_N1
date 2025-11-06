package com.bookhub.voucher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    // (Các query @Query giữ nguyên)
    @Query("SELECT v FROM Voucher v WHERE v.user IS NULL AND v.end_date >= ?1 AND v.quantity > 0")
    List<Voucher> findAvailablePublicVouchers(LocalDate today);

    @Query("SELECT v FROM Voucher v WHERE v.user.idUser = ?1 AND v.end_date >= ?2 AND v.quantity > 0")
    List<Voucher> findAvailableUserVouchers(Integer userId, LocalDate today);

    // **SỬA TÊN PHƯƠNG THỨC Ở ĐÂY:** Tìm theo codeName (không phân biệt hoa thường)
    Optional<Voucher> findByCodeNameIgnoreCase(String codeName); // Đổi về camelCase
}