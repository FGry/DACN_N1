package com.bookhub.address;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressDTO {

    // Ánh xạ id_address sang idAddress
    Integer idAddress;
    // Trường địa chỉ đầy đủ
    String address;
    // Số điện thoại
    String phone;

    public static AddressDTO fromEntity(Address address) {
        return AddressDTO.builder()
                .idAddress(address.getId_address())
                .address(address.getAddress())
                .phone(address.getPhone())
                .build();
    }
}