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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
            String cartItemsJson, String voucherCode, User loggedInUser, String paymentMethod) throws Exception {

        // 1. Đọc giỏ hàng
        List<CartItemDTO> cartItems = getCartItemsFromJson(cartItemsJson);
        if (cartItems.isEmpty()) throw new RuntimeException("Giỏ hàng rỗng.");

        // 2. Tạo đơn hàng cơ bản
        Order newOrder = buildBaseOrder(customerName, customerPhone, customerAddress, loggedInUser);
        newOrder.setOrderToken(UUID.randomUUID().toString()); // Tạo Token để tra cứu nhanh
        newOrder.setPayment_method(paymentMethod != null ? paymentMethod : "COD");

        Map<Integer, Product> productMap = getProductMap(cartItems);
        long subTotal = 0;
        List<OrderDetail> orderDetailsList = new ArrayList<>();

        // 3. Xử lý từng sản phẩm
        for (CartItemDTO cartItem : cartItems) {
            Product product = productMap.get(cartItem.getIdProducts());
            if (product == null || product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm không đủ hàng: " + (product != null ? product.getTitle() : "Unknown"));
            }

            // Trừ kho & Cộng lượt đã bán
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            int currentSold = (product.getSoldCount() == null) ? 0 : product.getSoldCount();
            product.setSoldCount(currentSold + cartItem.getQuantity());

            OrderDetail detail = buildOrderDetail(newOrder, product, cartItem.getQuantity());
            subTotal += detail.getTotal();
            orderDetailsList.add(detail);
        }

        // 4. Xử lý Voucher
        long discountAmount = 0L;
        Voucher appliedVoucher = null;

        if (StringUtils.hasText(voucherCode)) {
            try {
                appliedVoucher = voucherRepository.findByCodeNameIgnoreCase(voucherCode).orElse(null);
                if (appliedVoucher != null && appliedVoucher.getQuantity() > 0) {
                    BigDecimal subTotalBD = new BigDecimal(subTotal);
                    BigDecimal discountBD = voucherService.calculateDiscount(voucherCode, subTotalBD);
                    discountAmount = discountBD.longValue();
                    // Trừ số lượng voucher
                    appliedVoucher.setQuantity(appliedVoucher.getQuantity() - 1);
                } else {
                    appliedVoucher = null;
                }
            } catch (Exception e) {
                // Nếu lỗi voucher thì bỏ qua, vẫn cho đặt hàng giá gốc
                voucherCode = null;
            }
        }

        // 5. Lưu đơn hàng
        newOrder.setTotal(subTotal - discountAmount);
        newOrder.setDiscountAmount(discountAmount);
        newOrder.setVoucherCode(appliedVoucher != null ? voucherCode : null);
        newOrder.setVoucher(appliedVoucher);
        newOrder.setOrderDetails(orderDetailsList);

        Order savedOrder = orderRepository.save(newOrder);
        productRepository.saveAll(productMap.values()); // Lưu cập nhật kho

        return savedOrder;
    }

    // ==================================================================
    // 2. XÁC NHẬN THANH TOÁN (CONFIRM PAYMENT - PAYOS)
    // ==================================================================
    @Transactional
    public void confirmPayment(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Chỉ cập nhật nếu đơn hàng đang chờ thanh toán
        if ("PENDING".equalsIgnoreCase(order.getStatus_order())) {
            order.setStatus_order("CONFIRMED");
            // Lưu ý: Không setTotal(0) ở đây để giữ số liệu thống kê doanh thu.
            // Việc hiển thị 0đ sẽ do hàm mapToDTO xử lý.
            orderRepository.save(order);
        }
    }

    // ==================================================================
    // 3. THỐNG KÊ DOANH THU (DASHBOARD)
    // ==================================================================
    public RevenueStatsDTO getRevenueDashboardStats(Integer year) {
        Long totalRevenue = orderRepository.sumTotalDeliveredOrders(year).orElse(0L);
        Long totalOrders = orderRepository.countDeliveredOrders(year);

        // Lấy Top sản phẩm bán chạy (Cần OrderRepository hỗ trợ method này)
        List<ProductSaleStats> topProducts = orderRepository.findTopSellingProducts(year, PageRequest.of(0, 5));

        // Lấy dữ liệu biểu đồ theo tháng
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
    // 4. CÁC HÀM TRA CỨU & QUẢN LÝ (CRUD)
    // ==================================================================
    public OrderDTO findOrderById(Integer id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + id));
        return mapToDetailDTO(order);
    }

    public List<OrderDTO> findAllOrders() {
        return orderRepository.findAllWithUserAndDetails().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> findOrdersByUserId(Integer userId) {
        if (userId == null) return Collections.emptyList();
        return orderRepository.findByUserIdOrderByDateDesc(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> searchOrders(String searchTerm) {
        return orderRepository.searchOrders(searchTerm).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> filterOrders(String status) {
        if (status == null || status.isEmpty()) return findAllOrders();
        return orderRepository.findByStatus_orderIgnoreCase(status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(Integer id, String newStatus) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Lỗi ID"));
        order.setStatus_order(newStatus);
        orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Integer id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Lỗi ID"));
        if ("DELIVERED".equalsIgnoreCase(order.getStatus_order())
                || "CANCELLED".equalsIgnoreCase(order.getStatus_order())) {
            throw new RuntimeException("Không thể hủy đơn đã giao hoặc đã hủy.");
        }
        order.setStatus_order("CANCELLED");
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrderByToken(String orderToken) {
        return orderRepository.findByOrderToken(orderToken)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ."));
    }

    public Order getOrderById(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + id));
    }
    @Transactional(readOnly = true)
    public List<OrderDetailDTO> getOrderDetailsByOrder(Order order) {
        List<OrderDetail> entities = orderDetailRepository.findByOrder_Id_order(order.getId_order());
        return entities.stream().map(this::mapDetailToDTO).collect(Collectors.toList());
    }
    public List<OrderDetailDTO> getOrderDetailsByOrderId(Integer orderId) {
        List<OrderDetail> entities = orderDetailRepository.findByOrder_Id_order(orderId);
        return entities.stream().map(this::mapDetailToDTO).collect(Collectors.toList());
    }

    // ==================================================================
    // 5. PRIVATE HELPER & MAPPING METHODS
    // ==================================================================

    private List<CartItemDTO> getCartItemsFromJson(String cartItemsJson) {
        try {
            return objectMapper.readValue(cartItemsJson, new TypeReference<List<CartItemDTO>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc giỏ hàng.", e);
        }
    }

    private Order buildBaseOrder(String customerName, String customerPhone, String customerAddress, User loggedInUser) {
        Order newOrder = new Order();
        newOrder.setDate(LocalDate.now());
        newOrder.setStatus_order("PENDING");
        newOrder.setAddress(customerAddress);
        newOrder.setPhone(customerPhone);
        // Mặc định là COD, sẽ được ghi đè trong processOrder nếu có truyền vào
        newOrder.setPayment_method("COD");

        if (loggedInUser != null) {
            newOrder.setUser(loggedInUser);
            if (!loggedInUser.getUsername().equals(customerName)) {
                newOrder.setNote("Người nhận: " + customerName);
            }
        } else {
            newOrder.setUser(null);
            newOrder.setNote("Khách vãng lai: " + customerName);
        }
        return newOrder;
    }

    private Map<Integer, Product> getProductMap(List<CartItemDTO> cartItems) {
        List<Integer> productIds = cartItems.stream().map(CartItemDTO::getIdProducts).toList();
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getIdProducts, p -> p));
    }

    private OrderDetail buildOrderDetail(Order order, Product product, Integer quantity) {
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity((long) quantity);

        Long originalPrice = product.getPrice();
        Integer discountPercent = product.getDiscount() != null ? product.getDiscount() : 0;

        long finalPricePerUnit = originalPrice;
        if (discountPercent > 0) {
            BigDecimal priceBD = new BigDecimal(originalPrice);
            BigDecimal discountFactor = new BigDecimal(discountPercent).divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
            finalPricePerUnit = priceBD.subtract(priceBD.multiply(discountFactor)).longValue();
        }

        detail.setPrice_date(finalPricePerUnit);
        detail.setTotal(finalPricePerUnit * quantity);
        detail.setOrder(order);
        return detail;
    }

    // --- LOGIC QUAN TRỌNG: HIỂN THỊ TIỀN 0Đ KHI IN ĐƠN ---
    private OrderDTO mapToDTO(Order entity) {
        OrderDTO dto = new OrderDTO();
        dto.setIdOrder(entity.getId_order());
        dto.setOrderCode("#DH" + entity.getId_order());
        dto.setCustomerUsername(entity.getUser() != null ? entity.getUser().getUsername() : "Khách vãng lai");
        dto.setCustomerPhone(entity.getPhone());

        // LOGIC:
        // 1. Nếu là đơn Online (không phải COD) mà trạng thái là CONFIRMED/PAID -> Đã thanh toán -> Hiện 0đ
        // 2. Nếu là đơn bất kỳ mà trạng thái là DELIVERED (Đã giao) -> Đã thu tiền -> Hiện 0đ
        boolean isCOD = "COD".equalsIgnoreCase(entity.getPayment_method());
        boolean isPaidOnline = !isCOD && ("CONFIRMED".equalsIgnoreCase(entity.getStatus_order())
                || "PAID".equalsIgnoreCase(entity.getStatus_order()));
        boolean isDelivered = "DELIVERED".equalsIgnoreCase(entity.getStatus_order())
                || "COMPLETED".equalsIgnoreCase(entity.getStatus_order());

        if (isPaidOnline || isDelivered) {
            dto.setTotalAmount(0L);
            dto.setTotalAmountFormatted("0đ (Đã thanh toán)");
        } else {
            dto.setTotalAmount(entity.getTotal());
            dto.setTotalAmountFormatted(String.format("%,dđ", entity.getTotal()).replace(",", "."));
        }

        dto.setDiscountAmount(entity.getDiscountAmount() != null ? entity.getDiscountAmount() : 0L);
        dto.setDiscountAmountFormatted(String.format("%,dđ", dto.getDiscountAmount()).replace(",", "."));
        dto.setStatus(entity.getStatus_order());
        dto.setDate(entity.getDate());
        dto.setDateFormatted(entity.getDate().format(DATE_FORMATTER));

        // Tính tổng số lượng sản phẩm
        long totalQuantity = entity.getOrderDetails() != null ? entity.getOrderDetails().stream()
                .mapToLong(OrderDetail::getQuantity).sum() : 0;
        dto.setTotalProducts((int) totalQuantity);

        return dto;
    }

    private OrderDTO mapToDetailDTO(Order entity) {
        OrderDTO dto = mapToDTO(entity); // Tái sử dụng logic hiển thị tiền ở trên
        dto.setAddress(entity.getAddress());
        dto.setPaymentMethod(entity.getPayment_method());
        dto.setNote(entity.getNote());
        if(entity.getUser() != null) dto.setUserId(entity.getUser().getIdUser());

        dto.setProductDetails(entity.getOrderDetails().stream()
                .map(this::mapDetailToDTO)
                .collect(Collectors.toList()));
        return dto;
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
            dto.setProductImageUrl((product.getImages() != null && !product.getImages().isEmpty())
                    ? product.getImages().get(0).getImage_link() : "/images/default-book.png");
        }
        return dto;
    }
}