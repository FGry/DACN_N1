package com.bookhub.address;

import com.bookhub.user.User;
import com.bookhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user/address") // Dùng chung prefix cho cả API và Thao tác CRUD
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final UserService userService;

    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra xem có xác thực và không phải là người dùng ẩn danh không
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser")) {

            String email = authentication.getName();
            return userService.findUserByEmail(email);
        }
        return Optional.empty();
    }

    @GetMapping("/addresses")
    @ResponseBody
    public ResponseEntity<List<AddressDTO>> getUserAddresses() {
        Optional<User> currentUserOpt = getAuthenticatedUser();

        // Nếu người dùng chưa đăng nhập, trả về HTTP 401 Unauthorized
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
        }

        List<AddressDTO> addresses = addressService.getAddressesByUserId(currentUserOpt.get().getIdUser());

        return ResponseEntity.ok(addresses);
    }

    // ----------------------------------------------------------------------

    /**
     * MAPPING: Thêm hoặc Cập nhật Địa chỉ (Sử dụng RedirectAttributes để thông báo)
     * MAPPING: POST /user/address/add
     */
    @PostMapping("/add")
    public String addAddress(
            @RequestParam("fullAddressDetail") String fullAddressDetail,
            @RequestParam("phone") String phone,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUserOpt = getAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/logout"; // Yêu cầu đăng nhập lại
        }
        User currentUser = currentUserOpt.get();

        try {
            // Tạo DTO để truyền dữ liệu cho Service
            AddressDTO newAddressDto = new AddressDTO();
            newAddressDto.setUserId(currentUser.getIdUser());
            newAddressDto.setFullAddressDetail(fullAddressDetail);
            newAddressDto.setPhone(phone);

            addressService.saveOrUpdateAddress(newAddressDto);

            redirectAttributes.addFlashAttribute("addressSuccess", "Thêm địa chỉ mới thành công! ✅");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("addressError", "Lỗi khi thêm địa chỉ: " + e.getMessage());
        }

        // Chuyển hướng về trang profile và mở tab địa chỉ
        return "redirect:/user/profile#address-settings";
    }

    // ----------------------------------------------------------------------

    /** * MAPPING: Xóa Địa chỉ (Chuyển hướng về trang Profile)
     * MAPPING: POST /user/address/delete/{addressId}
     */
    @PostMapping("/delete/{addressId}") // Đổi tên biến để rõ ràng hơn
    public String deleteAddress(@PathVariable("addressId") Integer addressId, RedirectAttributes redirectAttributes) { // Nhận biến đã đổi tên

        Optional<User> currentUserOpt = getAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/logout";
        }

        Integer currentUserId = currentUserOpt.get().getIdUser();

        try {
            // Truyền ID địa chỉ và ID người dùng để Service xử lý logic xóa và quyền
            addressService.deleteAddress(addressId, currentUserId);
            redirectAttributes.addFlashAttribute("addressSuccess", "Đã xóa địa chỉ thành công! 🗑️");
        } catch (RuntimeException e) {
            // Bắt lỗi kiểm tra quyền/tồn tại/khóa ngoại từ Service
            redirectAttributes.addFlashAttribute("addressError", e.getMessage());
        } catch (Exception e) {
            // Bắt lỗi hệ thống khác
            redirectAttributes.addFlashAttribute("addressError", "Lỗi hệ thống khi xóa địa chỉ.");
        }

        return "redirect:/user/profile#address-settings";
    }
}