package com.bookhub.order;

import com.bookhub.cart.CartItemDTO;
import com.bookhub.product.Product; // THÊM
import com.bookhub.product.ProductRepository;
import com.bookhub.user.User; // THÊM
import com.bookhub.voucher.Voucher; // THÊM
import com.bookhub.voucher.VoucherRepository; // THÊM
import com.fasterxml.jackson.core.type.TypeReference; // THÊM
import com.fasterxml.jackson.databind.ObjectMapper; // THÊM
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate; // THÊM
import java.time.format.DateTimeFormatter;
import java.util.ArrayList; // THÊM
import java.util.List;
import java.util.Map; // THÊM
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    // === THÊM CÁC DEPENDENCY CẦN THIẾT ===
    private final OrderDetailRepository orderDetailRepository;
    private final VoucherRepository voucherRepository;
    private final ObjectMapper objectMapper; // Để đọc JSON

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ===============================================
    // === PHƯƠNG THỨC MỚI: XỬ LÝ ĐẶT HÀNG ===
    // ===============================================

    /**
     * Xử lý toàn bộ logic đặt hàng, bao gồm cả khách và người dùng đã đăng nhập.
     * @Transactional đảm bảo tất cả thao tác (trừ kho, lưu đơn) thành công, hoặc rollback nếu lỗi.
     */
    @Transactional(rollbackFor = Exception.class)
    public Order processOrder(
            String customerName, String customerPhone, String customerAddress,
            String cartItemsJson, String voucherCode, User loggedInUser) throws Exception {

        // 1. Đọc giỏ hàng từ JSON
        List<CartItemDTO> cartItems;
        try {
            cartItems = objectMapper.readValue(cartItemsJson, new TypeReference<List<CartItemDTO>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc dữ liệu giỏ hàng.", e);
        }

        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng của bạn đang rỗng.");
        }

        // 2. Tạo đối tượng Order
        Order newOrder = new Order();
        newOrder.setDate(LocalDate.now());
        newOrder.setStatus_order("PENDING"); // Trạng thái chờ xử lý
        newOrder.setAddress(customerAddress);
        newOrder.setPhone(customerPhone);
        newOrder.setPayment_method("COD"); // Tạm thời hardcode, bạn có thể thêm lựa chọn

        // 3. Gán thông tin khách hàng
        if (loggedInUser != null) {
            newOrder.setUser(loggedInUser);
            // Ghi chú nếu tên trên form khác tên tài khoản
            if (!loggedInUser.getUsername().equals(customerName)) {
                newOrder.setNote("Người nhận: " + customerName);
            }
        } else {
            // Xử lý cho khách (guest)
            newOrder.setUser(null);
            newOrder.setNote("Khách vãng lai: " + customerName);
        }

        // 4. Lấy sản phẩm từ DB và kiểm tra kho
        List<Integer> productIds = cartItems.stream().map(CartItemDTO::getIdProducts).toList();
        List<Product> productsFromDB = productRepository.findAllById(productIds);
        Map<Integer, Product> productMap = productsFromDB.stream()
                .collect(Collectors.toMap(Product::getIdProducts, p -> p));

        long subTotal = 0;
        List<OrderDetail> orderDetailsList = new ArrayList<>();

        for (CartItemDTO cartItem : cartItems) {
            Product product = productMap.get(cartItem.getIdProducts());
            if (product == null) {
                throw new RuntimeException("Sản phẩm '" + cartItem.getTitle() + "' không còn tồn tại.");
            }
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm '" + product.getTitle() + "' không đủ hàng trong kho (Chỉ còn " + product.getStockQuantity() + ").");
            }

            // 5. Tạo Chi tiết Đơn hàng (OrderDetail)
            OrderDetail detail = new OrderDetail();
            detail.setProduct(product);
            detail.setQuantity((long) cartItem.getQuantity());
            detail.setPrice_date(product.getPrice()); // Lấy giá hiện tại từ DB
            detail.setDiscount(product.getDiscount()); // Lưu % giảm giá (nếu có)

            // Tính tổng tiền cho dòng này
            long lineTotal = product.getPrice() * cartItem.getQuantity();
            if (product.getDiscount() != null && product.getDiscount() > 0) {
                lineTotal = (long) (lineTotal * (1 - (product.getDiscount() / 100.0)));
            }
            detail.setTotal(lineTotal);

            subTotal += lineTotal;
            detail.setOrder(newOrder); // Liên kết ngược lại Order
            orderDetailsList.add(detail);

            // 6. Cập nhật số lượng kho (trong bộ nhớ)
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
        }

        newOrder.setTotal(subTotal); // Gán tổng tiền tạm tính

        // 7. Xử lý Voucher (Nếu đăng nhập và có mã)
        Voucher appliedVoucher = null;
        if (loggedInUser != null && voucherCode != null && !voucherCode.trim().isEmpty()) {
            appliedVoucher = voucherRepository.findByCodeNameIgnoreCase(voucherCode)
                    .orElseThrow(() -> new RuntimeException("Mã voucher '" + voucherCode + "' không hợp lệ."));

            // Kiểm tra điều kiện voucher
            if (appliedVoucher.getQuantity() <= 0) throw new RuntimeException("Voucher đã hết lượt sử dụng.");
            if (LocalDate.now().isAfter(appliedVoucher.getEnd_date())) throw new RuntimeException("Voucher đã hết hạn.");
            if (subTotal < appliedVoucher.getMin_order_value()) {
                throw new RuntimeException("Đơn hàng chưa đủ " + String.format("%,dđ", appliedVoucher.getMin_order_value()) + " để dùng voucher này.");
            }

            // Tính toán giảm giá
            long discountAmount = 0;
            if ("FIXED".equals(appliedVoucher.getDiscountType())) {
                discountAmount = appliedVoucher.getDiscountValue();
            } else if ("PERCENT".equals(appliedVoucher.getDiscountType())) {
                discountAmount = (long) (subTotal * (appliedVoucher.getDiscountPercent() / 100.0));
                if (appliedVoucher.getMaxDiscount() != null && appliedVoucher.getMaxDiscount() > 0 && discountAmount > appliedVoucher.getMaxDiscount()) {
                    discountAmount = appliedVoucher.getMaxDiscount();
                }
            }

            // Áp dụng giảm giá
            newOrder.setTotal(subTotal - discountAmount);
            newOrder.setVoucher(appliedVoucher);

            // Cập nhật số lượng voucher (trong bộ nhớ)
            appliedVoucher.setQuantity(appliedVoucher.getQuantity() - 1);
        }

        // 8. Lưu tất cả vào DB (Transaction)
        newOrder.setOrderDetails(orderDetailsList); // Gán danh sách chi tiết vào đơn hàng

        Order savedOrder = orderRepository.save(newOrder); // Lưu Order (và OrderDetail nhờ CascadeType.ALL)

        productRepository.saveAll(productsFromDB); // Cập nhật lại số lượng kho

        if (appliedVoucher != null) {
            voucherRepository.save(appliedVoucher); // Cập nhật lại số lượng voucher
        }

        return savedOrder;
    }


    // ===============================================
    // === CÁC CHỨC NĂNG THỐNG KÊ (GIỮ NGUYÊN) ===
    // ===============================================

    /**
     * Lấy tổng doanh thu thuần (chỉ tính DELIVERED).
     */
    public long getTotalRevenue(Integer year) {
        return orderRepository.sumTotalDeliveredOrders(year)
                .orElse(0L);
    }

    /**
     * Lấy tổng số đơn hàng đã hoàn thành (DELIVERED).
     */
    public long getTotalDeliveredOrders(Integer year) {
        return orderRepository.countDeliveredOrders(year);
    }

    /**
     * Lấy dữ liệu Doanh thu theo tháng để hiển thị biểu đồ đường/cột.
     * Giá trị được trả về ở đơn vị Triệu VNĐ (Double) để giữ độ chính xác.
     */
    private List<RevenueStatsDTO.DataPoint> getMonthlyRevenueData(Integer year) {
        List<Object[]> rawData = orderRepository.findMonthlyRevenueAndProfit(year);

        return rawData.stream().map(row -> {
            Integer month = (Integer) row[0];
            Long revenue = (Long) row[1];
            String monthLabel = YearMonth.of(year, month).getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("vi-VN"));
            Double revenueInMillions = revenue / 1_000_000.0; // Giữ giá trị thập phân

            RevenueStatsDTO.DataPoint dataPoint = new RevenueStatsDTO.DataPoint();
            dataPoint.setLabel(monthLabel);
            dataPoint.setValue(revenueInMillions);

            return dataPoint;
        }).collect(Collectors.toList());
    }

    /**
     * Tổng hợp toàn bộ dữ liệu thống kê doanh thu cho Dashboard.
     */
    @Transactional(readOnly = true)
    public RevenueStatsDTO getRevenueDashboardStats(Integer year) {
        RevenueStatsDTO stats = new RevenueStatsDTO();
        stats.setTotalRevenue(getTotalRevenue(year));
        stats.setTotalDeliveredOrders(getTotalDeliveredOrders(year));
        List<ProductSaleStats> topProducts = orderRepository.findTopSellingProducts(year, PageRequest.of(0, 5));
        stats.setTopSellingProducts(topProducts);
        stats.setMonthlyRevenue(getMonthlyRevenueData(year));
        return stats;
    }


    // ===============================================
    // === CÁC HÀM CRUD & MAPPING CŨ (GIỮ NGUYÊN) ===
    // ===============================================

    public List<OrderDTO> findAllOrders() {
        List<Order> orders = orderRepository.findAllWithUserAndDetails();
        return orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO findOrderById(Integer id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        return mapToDetailDTO(order);
    }

    public List<OrderDTO> filterOrders(String status) {
        if (status == null || status.isEmpty()) {
            return findAllOrders();
        }
        return orderRepository.findByStatus_orderIgnoreCase(status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> searchOrders(String searchTerm) {
        return orderRepository.searchOrders(searchTerm).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }


    @Transactional
    public void updateOrderStatus(Integer id, String newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng để cập nhật trạng thái."));
        order.setStatus_order(newStatus);
        orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng để hủy."));
        if ("DELIVERED".equalsIgnoreCase(order.getStatus_order()) || "CANCELLED".equalsIgnoreCase(order.getStatus_order())) {
            throw new RuntimeException("Không thể hủy đơn hàng đã giao hoặc đã hủy.");
        }
        order.setStatus_order("CANCELLED");
        orderRepository.save(order);
    }

    // --- MAPPING HELPERS (GIỮ NGUYÊN) ---

    private OrderDTO mapToDTO(Order entity) {
        OrderDTO dto = new OrderDTO();
        dto.setIdOrder(entity.getId_order());
        dto.setOrderCode("#DH" + entity.getId_order());
        if (entity.getUser() != null) {
            dto.setCustomerUsername(entity.getUser().getUsername());
        } else {
            dto.setCustomerUsername("Khách vãng lai"); // Xử lý khách
        }
        dto.setCustomerPhone(entity.getPhone());
        dto.setTotalAmount(entity.getTotal());
        dto.setTotalAmountFormatted(String.format("%,dđ", entity.getTotal()).replace(",", "."));
        dto.setStatus(entity.getStatus_order());
        dto.setDate(entity.getDate());
        dto.setDateFormatted(entity.getDate().format(DATE_FORMATTER));

        if (entity.getOrderDetails() != null) {
            long count = entity.getOrderDetails().stream().mapToLong(OrderDetail::getQuantity).sum();
            dto.setTotalProducts((int) count);
        } else {
            dto.setTotalProducts(0);
        }
        return dto;
    }

    private OrderDTO mapToDetailDTO(Order entity) {
        OrderDTO dto = mapToDTO(entity);
        dto.setAddress(entity.getAddress());
        dto.setPaymentMethod(entity.getPayment_method());
        dto.setNote(entity.getNote());
        if(entity.getUser() != null) {
            dto.setUserId(entity.getUser().getIdUser());
        }

        List<OrderDetailDTO> detailDTOs = entity.getOrderDetails().stream().map(this::mapDetailToDTO).collect(Collectors.toList());
        dto.setProductDetails(detailDTOs);

        return dto;
    }

    private OrderDetailDTO mapDetailToDTO(OrderDetail detail) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setQuantity(detail.getQuantity().intValue());
        dto.setPriceAtDate(detail.getPrice_date());
        dto.setPriceAtDateFormatted(String.format("%,dđ", detail.getPrice_date()).replace(",", "."));

        if (detail.getProduct() != null) {
            dto.setProductName(detail.getProduct().getTitle());
            dto.setProductAuthor(detail.getProduct().getAuthor());
        }
        return dto;
    }
}