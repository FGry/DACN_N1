package com.bookhub.address;

import com.bookhub.user.User;
import com.bookhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/user/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final UserService userService;

    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser")) {

            String email = authentication.getName();
            return userService.findUserByEmail(email);
        }
        return Optional.empty();
    }

    @PostMapping("/add")
    public String addAddress(
            @RequestParam("fullAddressDetail") String fullAddressDetail,
            @RequestParam("phone") String phone,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUserOpt = getAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/logout";
        }
        User currentUser = currentUserOpt.get();

        try {
            AddressDTO newAddressDto = new AddressDTO();
            newAddressDto.setUserId(currentUser.getIdUser());
            newAddressDto.setFullAddressDetail(fullAddressDetail);
            newAddressDto.setPhone(phone);

            addressService.saveOrUpdateAddress(newAddressDto);

            redirectAttributes.addFlashAttribute("addressSuccess", "Thêm địa chỉ mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("addressError", "Lỗi khi thêm địa chỉ: " + e.getMessage());
        }

        return "redirect:/user/profile#address-settings";
    }

    /** MAPPING: Xử lý Xóa Địa chỉ (Chuyển từ UserController) */
    @PostMapping("/delete/{id}")
    public String deleteAddress(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {

        Optional<User> currentUserOpt = getAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/logout";
        }

        try {
            addressService.deleteAddress(id, currentUserOpt.get().getIdUser());
            redirectAttributes.addFlashAttribute("addressSuccess", "Đã xóa địa chỉ thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("addressError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("addressError", "Lỗi hệ thống khi xóa địa chỉ.");
        }

        return "redirect:/user/profile#address-settings";
    }
}
