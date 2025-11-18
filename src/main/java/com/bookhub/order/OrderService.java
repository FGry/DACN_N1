package com.bookhub.order;

import com.bookhub.cart.CartItemDTO;
import com.bookhub.product.Product;
import com.bookhub.product.ProductRepository;
import com.bookhub.user.User;
import com.bookhub.voucher.Voucher;
import com.bookhub.voucher.VoucherRepository;
import com.bookhub.voucher.VoucherService; // Dùng để TÁI XÁC THỰC VOUCHER
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Collections;
import java.math.BigDecimal;
import java.math.RoundingMode;

// Gửi file code hoàn chỉnh: OrderService.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherService voucherService; // Dùng để TÁI XÁC THỰC VOUCHER
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ===============================================
    // === PHƯƠNG THỨC MỚI: XỬ LÝ ĐẶT HÀNG (ĐÃ SỬA VOUCHER LOGIC) ===
    // ===============================================
    @Transactional(rollbackFor = Exception.class)
    public Order processOrder(
            String customerName, String customerPhone, String customerAddress,
            String cartItemsJson, String voucherCode, User loggedInUser) throws Exception {

        // 1. Đọc giỏ hàng từ JSON
        List<CartItemDTO> cartItems = getCartItemsFromJson(cartItemsJson);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng của bạn đang rỗng.");
        }

        // 2. Tạo đối tượng Order (tạm thời)
        Order newOrder = buildBaseOrder(customerName, customerPhone, customerAddress, loggedInUser);

        // 3. Lấy sản phẩm từ DB và kiểm tra kho, tính subTotal
        Map<Integer, Product> productMap = getProductMap(cartItems);
        long subTotal = 0;
        List<OrderDetail> orderDetailsList = new ArrayList<>();

        // Logic tính SubTotal và tạo OrderDetails
        for (CartItemDTO cartItem : cartItems) {
            Product product = productMap.get(cartItem.getIdProducts());
            if (product == null) {
                throw new RuntimeException("Sản phẩm '" + cartItem.getTitle() + "' không còn tồn tại.");
            }
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm '" + product.getTitle() + "' không đủ hàng trong kho (Chỉ còn " + product.getStockQuantity() + ").");
            }

            // Tạo Chi tiết Đơn hàng (OrderDetail)
            OrderDetail detail = buildOrderDetail(newOrder, product, cartItem.getQuantity());
            subTotal += detail.getTotal(); // Cộng tổng dòng đã tính (đã trừ giảm giá sản phẩm)
            orderDetailsList.add(detail);

            // Cập nhật số lượng kho (trong bộ nhớ)
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
        }

        newOrder.setTotal(subTotal); // Gán tổng tiền tạm tính (SubTotal)

        // 4. XỬ LÝ VOUCHER (RE-VALIDATE VÀ TÍNH TOÁN AN TOÀN)
        long discountAmount = 0L;
        Voucher appliedVoucher = null;

        if (StringUtils.hasText(voucherCode)) {
            // RE-VALIDATE VOUCHER SỬ DỤNG LOGIC CHUẨN TỪ VOUCHERSERVICE
            try {
                // Chuyển Long subTotal sang BigDecimal để tính toán chính xác
                BigDecimal subTotalBD = new BigDecimal(subTotal);

                // GỌI LOGIC TÍNH TOÁN CHUẨN TỪ VOUCHERSERVICE
                BigDecimal discountBD = voucherService.calculateDiscount(voucherCode, subTotalBD);
                discountAmount = discountBD.longValue();

                // Chỉ lấy entity nếu tính toán thành công để link
                appliedVoucher = voucherRepository.findByCodeNameIgnoreCase(voucherCode).orElse(null);

            } catch (RuntimeException e) {
                // Nếu voucher không hợp lệ, không áp dụng và ghi log
                System.err.println("Cảnh báo: Mã voucher " + voucherCode + " bị từ chối khi submit: " + e.getMessage());
                voucherCode = null;
            }
        }

        // 5. ÁP DỤNG GIẢM GIÁ CUỐI CÙNG VÀ GÁN VOUCHER CHO ORDER
        newOrder.setDiscountAmount(discountAmount); // LƯU SỐ TIỀN GIẢM
        newOrder.setVoucherCode(voucherCode); // LƯU MÃ CODE
        newOrder.setVoucher(appliedVoucher); // LƯU ENTITY VOUCHER (nếu có)
        newOrder.setTotal(subTotal - discountAmount); // LƯU TỔNG TIỀN CUỐI CÙNG

        // 6. Lưu tất cả vào DB (Transaction)
        newOrder.setOrderDetails(orderDetailsList);

        Order savedOrder = orderRepository.save(newOrder);
        productRepository.saveAll(productMap.values()); // Cập nhật lại số lượng kho

        // 7. GIẢM SỐ LƯỢNG VOUCHER (CHỈ GỌI HÀM GIẢM SỐ LƯỢNG)
        if (StringUtils.hasText(voucherCode)) {
            voucherService.reduceVoucherQuantity(voucherCode);
        }

        return savedOrder;
    }

    // ===============================================
    // === HELPER METHODS (Được tạo từ logic cũ) ===
    // ===============================================

    private List<CartItemDTO> getCartItemsFromJson(String cartItemsJson) {
        try {
            return objectMapper.readValue(cartItemsJson, new TypeReference<List<CartItemDTO>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc dữ liệu giỏ hàng.", e);
        }
    }

    private Order buildBaseOrder(String customerName, String customerPhone, String customerAddress, User loggedInUser) {
        Order newOrder = new Order();
        newOrder.setDate(LocalDate.now());
        newOrder.setStatus_order("PENDING");
        newOrder.setAddress(customerAddress);
        newOrder.setPhone(customerPhone);
        newOrder.setPayment_method("COD");
        newOrder.setDiscountAmount(0L); // Mặc định là 0

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
        List<Product> productsFromDB = productRepository.findAllById(productIds);
        return productsFromDB.stream()
                .collect(Collectors.toMap(Product::getIdProducts, p -> p));
    }

    private OrderDetail buildOrderDetail(Order order, Product product, Integer quantity) {
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity((long) quantity);
        detail.setPrice_date(product.getPrice());
        detail.setDiscount(product.getDiscount());

        // Tính tổng tiền cho dòng này (đã bao gồm giảm giá sản phẩm)
        long lineTotal = product.getPrice() * quantity;
        if (product.getDiscount() != null && product.getDiscount() > 0) {
            // Tính toán giảm giá %
            BigDecimal totalBD = new BigDecimal(lineTotal);
            BigDecimal discountPercent = new BigDecimal(product.getDiscount()).divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
            BigDecimal discountAmount = totalBD.multiply(discountPercent).setScale(0, RoundingMode.HALF_UP);
            lineTotal = totalBD.subtract(discountAmount).longValue();
        }
        detail.setTotal(lineTotal);
        detail.setOrder(order);
        return detail;
    }

    // ===============================================
    // === CÁC CHỨC NĂNG THỐNG KÊ, CRUD & MAPPING CŨ (GIỮ NGUYÊN) ===
    // ===============================================
    // NOTE: Các phương thức này nên giữ nguyên logic ban đầu của bạn.

    public long getTotalRevenue(Integer year) {
        return orderRepository.sumTotalDeliveredOrders(year).orElse(0L);
    }
    public long getTotalDeliveredOrders(Integer year) {
        return orderRepository.countDeliveredOrders(year);
    }
    private List<RevenueStatsDTO.DataPoint> getMonthlyRevenueData(Integer year) {
        List<Object[]> rawData = orderRepository.findMonthlyRevenueAndProfit(year);
        return rawData.stream().map(row -> {
            Integer month = (Integer) row[0];
            Long revenue = (Long) row[1];
            String monthLabel = YearMonth.of(year, month).getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("vi-VN"));
            Double revenueInMillions = revenue / 1_000_000.0;
            RevenueStatsDTO.DataPoint dataPoint = new RevenueStatsDTO.DataPoint();
            dataPoint.setLabel(monthLabel);
            dataPoint.setValue(revenueInMillions);
            return dataPoint;
        }).collect(Collectors.toList());
    }
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

    public List<OrderDTO> findOrdersByUserId(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Order> orders = orderRepository.findByUserIdOrderByDateDesc(userId);
        return orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Order getOrderById(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
    }

    public List<OrderDetailDTO> getOrderDetailsByOrderId(Integer orderId) {
        List<OrderDetail> entities = orderDetailRepository.findByOrder_Id_order(orderId);
        return entities.stream().map(this::mapDetailToDTO).collect(Collectors.toList());
    }


    // --- MAPPING HELPERS (GIỮ NGUYÊN) ---

    private OrderDTO mapToDTO(Order entity) {
        OrderDTO dto = new OrderDTO();
        dto.setIdOrder(entity.getId_order());
        dto.setOrderCode("#DH" + entity.getId_order());
        if (entity.getUser() != null) {
            dto.setCustomerUsername(entity.getUser().getUsername());
        } else {
            dto.setCustomerUsername("Khách vãng lai");
        }
        dto.setCustomerPhone(entity.getPhone());

        // Cập nhật: TotalAmount nên lấy từ total đã trừ giảm giá
        dto.setTotalAmount(entity.getTotal());
        dto.setTotalAmountFormatted(String.format("%,dđ", entity.getTotal()).replace(",", "."));

        // Thêm trường giảm giá
        dto.setDiscountAmount(entity.getDiscountAmount() != null ? entity.getDiscountAmount() : 0L);
        dto.setDiscountAmountFormatted(String.format("%,dđ", dto.getDiscountAmount()).replace(",", "."));

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

        List<OrderDetailDTO> detailDTOs = entity.getOrderDetails().stream()
                .map(this::mapDetailToDTO)
                .collect(Collectors.toList());
        dto.setProductDetails(detailDTOs);

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

            if (product.getImages() != null && !product.getImages().isEmpty()) {
                dto.setProductImageUrl(product.getImages().get(0).getImage_link());
            } else {
                dto.setProductImageUrl("/images/default-book.png");
            }
        }
        return dto;
    }
}