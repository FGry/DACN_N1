package com.bookhub.order;

import com.bookhub.cart.CartItemDTO;
import com.bookhub.product.Product;
import com.bookhub.product.ProductRepository;
import com.bookhub.user.User;
import com.bookhub.voucher.Voucher;
import com.bookhub.voucher.VoucherRepository;
import com.bookhub.voucher.VoucherService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;
import com.bookhub.product.ImageProduct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Collections;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherService voucherService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==================================================================
    // 1. XỬ LÝ ĐẶT HÀNG (PROCESS ORDER)
    // ==================================================================
    @Transactional(rollbackFor = Exception.class)
    public Order processOrder(
            String customerName, String customerPhone, String customerAddress,
            String cartItemsJson, String voucherCode, User loggedInUser) throws Exception {

        List<CartItemDTO> cartItems = getCartItemsFromJson(cartItemsJson);
        if (cartItems.isEmpty()) throw new RuntimeException("Giỏ hàng rỗng.");

        Order newOrder = buildBaseOrder(customerName, customerPhone, customerAddress, loggedInUser);

        // === TẠO TOKEN ===
        newOrder.setOrderToken(UUID.randomUUID().toString());
        // =================

        Map<Integer, Product> productMap = getProductMap(cartItems);
        // Khởi tạo subTotal là 0
        long subTotal = 0;
        List<OrderDetail> orderDetailsList = new ArrayList<>();

        for (CartItemDTO cartItem : cartItems) {
            Product product = productMap.get(cartItem.getIdProducts());
            if (product == null || product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm không đủ hàng.");
            }
            // Gọi buildOrderDetail (đã sửa) để tính đúng giá và tổng dòng
            OrderDetail detail = buildOrderDetail(newOrder, product, cartItem.getQuantity());
            // Cộng tổng dòng (đã tính giảm giá sản phẩm)
            subTotal += detail.getTotal();
            orderDetailsList.add(detail);
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
        }

        // Tạm thời gán subTotal đã tính (đã áp dụng giảm giá sản phẩm)
        newOrder.setTotal(subTotal);

        long discountAmount = 0L;
        Voucher appliedVoucher = null;
        if (StringUtils.hasText(voucherCode)) {
            try {
                BigDecimal subTotalBD = new BigDecimal(subTotal);
                BigDecimal discountBD = voucherService.calculateDiscount(voucherCode, subTotalBD);
                discountAmount = discountBD.longValue();
                appliedVoucher = voucherRepository.findByCodeNameIgnoreCase(voucherCode).orElse(null);
            } catch (RuntimeException e) {
                voucherCode = null;
            }
        }

        newOrder.setDiscountAmount(discountAmount);
        newOrder.setVoucherCode(voucherCode);
        newOrder.setVoucher(appliedVoucher);

        // Tổng cuối cùng = SubTotal (đã giảm SP) - Discount Voucher
        newOrder.setTotal(subTotal - discountAmount);
        newOrder.setOrderDetails(orderDetailsList);

        Order savedOrder = orderRepository.save(newOrder);
        productRepository.saveAll(productMap.values());

        if (StringUtils.hasText(voucherCode)) {
            voucherService.reduceVoucherQuantity(voucherCode);
        }
        return savedOrder;
    }

    // ==================================================================
    // 2. CÁC HÀM CHO QR CODE & GUEST (KHÁCH VÃNG LAI)
    // ==================================================================
    @Transactional(readOnly = true)
    public Order getOrderByToken(String orderToken) {
        return orderRepository.findByOrderToken(orderToken)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ."));
    }

    @Transactional(readOnly = true)
    public List<OrderDetailDTO> getOrderDetailsByOrder(Order order) {
        List<OrderDetail> entities = orderDetailRepository.findByOrder_Id_order(order.getId_order());
        return entities.stream().map(this::mapDetailToDTO).collect(Collectors.toList());
    }

    // ==================================================================
    // 3. CÁC HÀM CHO USER & ADMIN (LỊCH SỬ, CHI TIẾT, QUẢN LÝ)
    // ==================================================================

    @Transactional(readOnly = true)
    public List<OrderDetailDTO> getOrderDetailsByOrderId(Integer orderId) {
        List<OrderDetail> entities = orderDetailRepository.findByOrder_Id_order(orderId);
        return entities.stream().map(this::mapDetailToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
    }

    public List<OrderDTO> searchOrders(String searchTerm) {
        return orderRepository.searchOrders(searchTerm).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<OrderDTO> filterOrders(String status) {
        return (status == null || status.isEmpty()) ? findAllOrders() : orderRepository.findByStatus_orderIgnoreCase(status).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<OrderDTO> findAllOrders() {
        return orderRepository.findAllWithUserAndDetails().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public OrderDTO findOrderById(Integer id) {
        Order order = orderRepository.findByIdWithDetails(id).orElseThrow(() -> new RuntimeException("Not found"));
        return mapToDetailDTO(order);
    }

    @Transactional
    public void updateOrderStatus(Integer id, String newStatus) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus_order(newStatus);
        orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Integer id) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus_order("CANCELLED");
        orderRepository.save(order);
    }

    public List<OrderDTO> findOrdersByUserId(Integer userId) {
        return (userId == null) ? Collections.emptyList() : orderRepository.findByUserIdOrderByDateDesc(userId).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ==================================================================
    // 4. HÀM THỐNG KÊ DOANH THU (ADMIN REVENUE)
    // ==================================================================
    public RevenueStatsDTO getRevenueDashboardStats(Integer year) {
        Long totalRevenue = orderRepository.sumTotalDeliveredOrders(year).orElse(0L);
        Long totalOrders = orderRepository.countDeliveredOrders(year);
        List<ProductSaleStats> topProducts = orderRepository.findTopSellingProducts(year, PageRequest.of(0, 5));
        List<Object[]> monthlyData = orderRepository.findMonthlyRevenueAndProfit(year);
        List<RevenueStatsDTO.DataPoint> chartData = new ArrayList<>();

        Map<Integer, Double> monthMap = new HashMap<>();
        for (int i = 1; i <= 12; i++) monthMap.put(i, 0.0);
        for (Object[] row : monthlyData) {
            int month = (int) row[0];
            long revenue = (long) row[1];
            monthMap.put(month, (double) revenue);
        }
        for (int i = 1; i <= 12; i++) {
            String label = "Tháng " + i;
            chartData.add(new RevenueStatsDTO.DataPoint(label, monthMap.get(i)));
        }

        return RevenueStatsDTO.builder()
                .totalRevenue(totalRevenue)
                .totalDeliveredOrders(totalOrders)
                .topSellingProducts(topProducts)
                .monthlyRevenue(chartData)
                .build();
    }

    // ==================================================================
    // 5. PRIVATE HELPER METHODS
    // ==================================================================

    private List<CartItemDTO> getCartItemsFromJson(String cartItemsJson) {
        try { return objectMapper.readValue(cartItemsJson, new TypeReference<List<CartItemDTO>>() {}); }
        catch (Exception e) { throw new RuntimeException("Lỗi đọc giỏ hàng.", e); }
    }

    private Order buildBaseOrder(String customerName, String customerPhone, String customerAddress, User loggedInUser) {
        Order newOrder = new Order();
        newOrder.setDate(LocalDate.now());
        newOrder.setStatus_order("PENDING");
        newOrder.setAddress(customerAddress);
        newOrder.setPhone(customerPhone);
        newOrder.setPayment_method("COD");
        if (loggedInUser != null) {
            newOrder.setUser(loggedInUser);
            if (!loggedInUser.getUsername().equals(customerName)) newOrder.setNote("Người nhận: " + customerName);
        } else {
            newOrder.setUser(null);
            newOrder.setNote("Khách vãng lai: " + customerName);
        }
        return newOrder;
    }

    private Map<Integer, Product> getProductMap(List<CartItemDTO> cartItems) {
        List<Integer> productIds = cartItems.stream().map(CartItemDTO::getIdProducts).toList();
        return productRepository.findAllById(productIds).stream().collect(Collectors.toMap(Product::getIdProducts, p -> p));
    }

    /**
     * Phương thức tạo OrderDetail, tính toán giá cuối cùng đã áp dụng giảm giá sản phẩm.
     */
    private OrderDetail buildOrderDetail(Order order, Product product, Integer quantity) {
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity((long) quantity);

        // Lấy giá gốc sản phẩm
        Long originalPrice = product.getPrice();
        // Lấy % giảm giá sản phẩm
        Integer discountPercent = product.getDiscount() != null ? product.getDiscount() : 0;

        // B1: TÍNH TOÁN GIÁ CUỐI CÙNG CHO TỪNG SẢN PHẨM (Đã áp dụng giảm giá sản phẩm)
        long finalPricePerUnit = originalPrice;
        if (discountPercent > 0) {
            BigDecimal priceBD = new BigDecimal(originalPrice);
            // Chia lấy số thập phân chính xác cho % giảm giá
            BigDecimal discountFactor = new BigDecimal(discountPercent).divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
            // Tính giá cuối cùng
            finalPricePerUnit = priceBD.subtract(priceBD.multiply(discountFactor)).longValue();
        }

        // B2: Gán GIÁ CUỐI CÙNG đã giảm giá vào price_date để hiển thị đúng trên trang success
        detail.setPrice_date(finalPricePerUnit);

        // B3: Tính tổng dòng dựa trên giá cuối cùng
        long lineTotal = finalPricePerUnit * quantity;

        detail.setTotal(lineTotal);
        detail.setOrder(order);
        return detail;
    }

    private OrderDetailDTO mapDetailToDTO(OrderDetail detail) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setQuantity(detail.getQuantity().intValue());
        dto.setPriceAtDate(detail.getPrice_date());
        dto.setPriceAtDateFormatted(String.format("%,dđ", detail.getPrice_date()).replace(",", "."));
        Product product = detail.getProduct();
        if (product != null) {
            dto.setProductName(product.getTitle());
            dto.setProductAuthor(product.getAuthor());
            dto.setProductImageUrl((product.getImages() != null && !product.getImages().isEmpty()) ? product.getImages().get(0).getImage_link() : "/images/default-book.png");
        }
        return dto;
    }

    private OrderDTO mapToDTO(Order entity) {
        OrderDTO dto = new OrderDTO();
        dto.setIdOrder(entity.getId_order());
        dto.setOrderCode("#DH" + entity.getId_order());
        dto.setCustomerUsername(entity.getUser() != null ? entity.getUser().getUsername() : "Khách vãng lai");
        dto.setCustomerPhone(entity.getPhone());
        dto.setTotalAmount(entity.getTotal());
        dto.setTotalAmountFormatted(String.format("%,dđ", entity.getTotal()).replace(",", "."));
        dto.setDiscountAmount(entity.getDiscountAmount() != null ? entity.getDiscountAmount() : 0L);
        dto.setDiscountAmountFormatted(String.format("%,dđ", dto.getDiscountAmount()).replace(",", "."));
        dto.setStatus(entity.getStatus_order());
        dto.setDate(entity.getDate());
        dto.setDateFormatted(entity.getDate().format(DATE_FORMATTER));
        return dto;
    }

    private OrderDTO mapToDetailDTO(Order entity) {
        OrderDTO dto = mapToDTO(entity);
        dto.setAddress(entity.getAddress());
        dto.setPaymentMethod(entity.getPayment_method());
        dto.setNote(entity.getNote());
        if(entity.getUser() != null) dto.setUserId(entity.getUser().getIdUser());
        dto.setProductDetails(entity.getOrderDetails().stream().map(this::mapDetailToDTO).collect(Collectors.toList()));
        return dto;
    }
}