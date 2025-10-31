package com.bookhub.voucher;

import com.bookhub.user.User;
import com.bookhub.user.UserRepository;
import com.bookhub.voucher.Voucher;
import com.bookhub.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;
    private final UserRepository userRepository;

    @GetMapping
    public String listVouchers(Model model) {
        if (!model.containsAttribute("voucher")) {
            model.addAttribute("voucher", new Voucher());
        }
        List<Voucher> vouchers = voucherService.findAllForAdmin();
        model.addAttribute("vouchers", vouchers);
        if (!model.containsAttribute("openFormModal")) {
            model.addAttribute("openFormModal", false);
        }
        return "admin/voucher";
    }

    @GetMapping("/edit/{id}")
    @ResponseBody
    public Voucher getVoucherForEdit(@PathVariable("id") Integer id) {
        return voucherService.findByIdForAdmin(id);
    }

    @PostMapping("/save")
    public String saveVoucher(@ModelAttribute("voucher") Voucher voucher,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        try {
            // 1. Handle User assignment: LUÔN THIẾT LẬP LÀ VOUCHER CÔNG KHAI
            voucher.setUser(null);

            // 2. Validation cơ bản về ngày tháng (BẮT LỖI)
            if (voucher.getStart_date() == null || voucher.getEnd_date() == null) {
                throw new RuntimeException("Ngày bắt đầu và ngày kết thúc không được để trống.");
            }
            if (voucher.getEnd_date().isBefore(voucher.getStart_date())) {
                throw new RuntimeException("Ngày kết thúc phải xảy ra sau ngày bắt đầu."); // LỖI LOGIC
            }

            // 3. Kiểm tra và tối ưu hóa logic dữ liệu theo loại giảm giá
            if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
                voucher.setDiscountValue(null);
                if (voucher.getDiscountPercent() == null || voucher.getDiscountPercent() < 0 || voucher.getDiscountPercent() > 100) {
                    throw new RuntimeException("Phần trăm giảm giá phải nằm trong khoảng từ 0 đến 100."); // TIẾNG VIỆT
                }
                if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount() < 0) {
                    throw new RuntimeException("Giá trị giảm tối đa không được là số âm."); // TIẾNG VIỆT
                }
                // THIẾT LẬP discountValueStr TỰ ĐỘNG
                voucher.setDiscountValueStr(voucher.getDiscountPercent() + "%");

            } else if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) {
                voucher.setDiscountPercent(null);
                voucher.setMaxDiscount(null);
                if (voucher.getDiscountValue() == null || voucher.getDiscountValue() <= 0) {
                    throw new RuntimeException("Giá trị giảm cố định phải lớn hơn 0."); // TIẾNG VIỆT
                }
                // THIẾT LẬP discountValueStr TỰ ĐỘNG (Giữ nguyên logic cũ)
                if (voucher.getDiscountValue() >= 1000000L) {
                    voucher.setDiscountValueStr(String.format("%.1fM", voucher.getDiscountValue() / 1000000.0));
                } else if (voucher.getDiscountValue() >= 1000L) {
                    voucher.setDiscountValueStr(String.format("%,dK", voucher.getDiscountValue() / 1000L).replace(",", "."));
                } else {
                    voucher.setDiscountValueStr(voucher.getDiscountValue().toString() + "đ");
                }

            } else {
                throw new RuntimeException("Vui lòng chọn loại giảm giá hợp lệ (Phần trăm hoặc Giá trị cố định)."); // TIẾNG VIỆT
            }

            // Common validations
            if (voucher.getMin_order_value() != null && voucher.getMin_order_value() < 0) {
                throw new RuntimeException("Giá trị đơn hàng tối thiểu không được là số âm."); // TIẾNG VIỆT
            }
            if (voucher.getMin_order_value() == null) voucher.setMin_order_value(0L);
            if (voucher.getQuantity() == null || voucher.getQuantity() < 0) {
                throw new RuntimeException("Số lượng voucher không được là số âm."); // TIẾNG VIỆT
            }

            // 4. Call service to save
            voucherService.saveAdminVoucher(voucher);

            redirectAttributes.addFlashAttribute("successMessage", "Voucher đã được lưu thành công!"); // TIẾNG VIỆT
            return "redirect:/admin/vouchers";

        } catch (RuntimeException e) {
            // Handle errors
            model.addAttribute("errorMessage", "Lỗi: " + e.getMessage()); // TIẾNG VIỆT

            model.addAttribute("vouchers", voucherService.findAllForAdmin());
            model.addAttribute("voucher", voucher);
            model.addAttribute("openFormModal", true);
            return "admin/voucherManage";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            voucherService.deleteAdminVoucherById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Voucher ID " + id + " đã được xóa thành công!"); // TIẾNG VIỆT
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa voucher: " + e.getMessage()); // TIẾNG VIỆT
        }
        return "redirect:/admin/vouchers";
    }
}