package com.bookhub.order;

import com.bookhub.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.format.DateTimeFormatter;
import java.util.List;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ===============================================
    // === CHỨC NĂNG THỐNG KÊ MỚI (DELIVERED) ===
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
        // Gọi truy vấn đã được thêm vào OrderRepository
        List<Object[]> rawData = orderRepository.findMonthlyRevenueAndProfit(year);

        return rawData.stream().map(row -> {
            Integer month = (Integer) row[0];
            Long revenue = (Long) row[1];

            // Định dạng tên tháng tiếng Việt
            String monthLabel = YearMonth.of(year, month).getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("vi-VN"));

            // 🌟 KHẮC PHỤC LỖI LÀM TRÒN: Dùng 1_000_000.0 (Double) để chia và giữ lại giá trị thập phân
            Double revenueInMillions = revenue / 1_000_000.0;

            RevenueStatsDTO.DataPoint dataPoint = new RevenueStatsDTO.DataPoint();
            dataPoint.setLabel(monthLabel);
            dataPoint.setValue(revenueInMillions); // Gán giá trị Double

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

        // 2. Sản phẩm bán chạy (Top 5)
        List<ProductSaleStats> topProducts = orderRepository.findTopSellingProducts(year, PageRequest.of(0, 5));
        stats.setTopSellingProducts(topProducts);

        // 3. Doanh thu theo tháng (Dữ liệu thực tế cho biểu đồ)
        stats.setMonthlyRevenue(getMonthlyRevenueData(year));

        return stats;
    }


    // ===============================================
    // === CÁC HÀM CRUD & MAPPING CŨ ===
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

    // --- MAPPING HELPERS ---

    private OrderDTO mapToDTO(Order entity) {
        OrderDTO dto = new OrderDTO();
        dto.setIdOrder(entity.getId_order());
        dto.setOrderCode("#DH" + entity.getId_order());
        dto.setCustomerUsername(entity.getUser().getUsername());
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
        dto.setUserId(entity.getUser().getIdUser());

        List<OrderDetailDTO> detailDTOs = entity.getOrderDetails().stream().map(this::mapDetailToDTO).collect(Collectors.toList());
        dto.setProductDetails(detailDTOs);

        return dto;
    }

    private OrderDetailDTO mapDetailToDTO(OrderDetail detail) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setQuantity(detail.getQuantity().intValue());
        // Giá sản phẩm tại thời điểm đặt hàng (Lỗi giá trị sai là do dữ liệu DB)
        dto.setPriceAtDate(detail.getPrice_date());
        dto.setPriceAtDateFormatted(String.format("%,dđ", detail.getPrice_date()).replace(",", "."));

        if (detail.getProduct() != null) {
            dto.setProductName(detail.getProduct().getTitle());
            dto.setProductAuthor(detail.getProduct().getAuthor());
        }
        return dto;
    }
}