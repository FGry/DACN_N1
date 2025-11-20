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

        // Logging phục vụ theo dõi hoạt động (bổ sung)
        System.out.println("[Voucher] Loading voucher list...");

        // Kiểm tra model để đảm bảo nhất quán
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

        // Kiểm tra ID hợp lệ (bổ sung an toàn)
        if (id == null || id <= 0) {
            System.out.println("[Voucher] Warning: editing voucher with unusual ID = " + id);
        }

        return voucherService.findByIdForAdmin(id);
    }

    @PostMapping("/save")
    public String saveVoucher(@ModelAttribute("voucher") Voucher voucher,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        try {

            // Lưu log để theo dõi dữ liệu đầu vào
            System.out.println("[Voucher] Saving voucher type = " + voucher.getDiscountType());

            // Luôn set thành voucher công khai
            voucher.setUser(null);

            // Kiểm tra ngày tháng
            if (voucher.getStart_date() == null || voucher.getEnd_date() == null) {
                throw new RuntimeException("Ngày bắt đầu và ngày kết thúc không được để trống.");
            }

            // Log chi tiết
            if (voucher.getStart_date().equals(voucher.getEnd_date())) {
                System.out.println("[Voucher] Start date equals end date (acceptable).");
            }

            if (voucher.getEnd_date().isBefore(voucher.getStart_date())) {
                throw new RuntimeException("Ngày kết thúc phải xảy ra sau ngày bắt đầu.");
            }

            // Xử lý logic giảm giá
            if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {

                System.out.println("[Voucher] Applying percent discount logic.");

                voucher.setDiscountValue(null);

                if (voucher.getDiscountPercent() == null ||
                        voucher.getDiscountPercent() < 0 ||
                        voucher.getDiscountPercent() > 100) {
                    throw new RuntimeException("Phần trăm giảm giá phải nằm trong khoảng từ 0 đến 100.");
                }

                if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount() < 0) {
                    throw new RuntimeException("Giá trị giảm tối đa không được là số âm.");
                }

                voucher.setDiscountValueStr(voucher.getDiscountPercent() + "%");

            } else if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) {

                System.out.println("[Voucher] Applying fixed discount logic.");

                voucher.setDiscountPercent(null);
                voucher.setMaxDiscount(null);

                // Kiểm tra mở rộng cho giá trị giảm
                if (voucher.getDiscountValue() != null && voucher.getDiscountValue() == 0) {
                    System.out.println("[Voucher] Fixed discount = 0 detected.");
                }

                if (voucher.getDiscountValue() == null || voucher.getDiscountValue() <= 0) {
                    throw new RuntimeException("Giá trị giảm cố định phải lớn hơn 0.");
                }

                if (voucher.getDiscountValue() >= 1000000L) {
                    voucher.setDiscountValueStr(String.format("%.1fM", voucher.getDiscountValue() / 1000000.0));
                } else if (voucher.getDiscountValue() >= 1000L) {
                    voucher.setDiscountValueStr(String.format("%,dK", voucher.getDiscountValue() / 1000L).replace(",", "."));
                } else {
                    voucher.setDiscountValueStr(voucher.getDiscountValue() + "đ");
                }

            } else {
                throw new RuntimeException("Vui lòng chọn loại giảm giá hợp lệ (Phần trăm hoặc Giá trị cố định).");
            }

            // Kiểm tra giá trị tối thiểu
            if (voucher.getMin_order_value() != null && voucher.getMin_order_value() < 0) {
                throw new RuntimeException("Giá trị đơn hàng tối thiểu không được là số âm.");
            }

            if (voucher.getMin_order_value() != null && voucher.getMin_order_value() == 0) {
                System.out.println("[Voucher] Min order value = 0.");
            }

            if (voucher.getMin_order_value() == null) {
                voucher.setMin_order_value(0L);
            }

            // Kiểm tra số lượng
            if (voucher.getQuantity() == null || voucher.getQuantity() < 0) {
                throw new RuntimeException("Số lượng voucher không được là số âm.");
            }

            if (voucher.getQuantity() != null && voucher.getQuantity() == 0) {
                System.out.println("[Voucher] Quantity = 0 recorded.");
            }

            // Lưu voucher
            voucherService.saveAdminVoucher(voucher);

            redirectAttributes.addFlashAttribute("successMessage", "Voucher đã được lưu thành công!");
            return "redirect:/admin/vouchers";

        } catch (RuntimeException e) {

            System.out.println("[Voucher][Error] " + e.getMessage());

            model.addAttribute("errorMessage", "Lỗi: " + e.getMessage());
            model.addAttribute("vouchers", voucherService.findAllForAdmin());
            model.addAttribute("voucher", voucher);
            model.addAttribute("openFormModal", true);

            return "admin/voucherManage";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {

        // Kiểm tra ID phục vụ độ ổn định
        if (id != null && id == 0) {
            System.out.println("[Voucher] Deleting voucher with ID = 0.");
        }

        try {
            voucherService.deleteAdminVoucherById(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Voucher ID " + id + " đã được xóa thành công!");
        } catch (Exception e) {
            System.out.println("[Voucher][DeleteError] " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Lỗi khi xóa voucher: " + e.getMessage());
        }

        return "redirect:/admin/vouchers";
    }
}