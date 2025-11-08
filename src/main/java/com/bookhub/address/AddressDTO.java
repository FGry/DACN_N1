package com.bookhub.address;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {

    private Integer idAddress;
    private String fullAddressDetail;
    private String phone;

    private Integer userId;

    public static AddressDTO fromEntity(Address address) {
        if (address == null) return null;
        return AddressDTO.builder()
                .idAddress(address.getIdAddress())
                .fullAddressDetail(address.getFullAddressDetail())
                .phone(address.getPhone())
                .userId(address.getUser() != null ? address.getUser().getIdUser() : null)
                .build();
    }
}