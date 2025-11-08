
package com.bookhub.address;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Integer> {

    List<Address> findByUser_IdUser(Integer userId);
}