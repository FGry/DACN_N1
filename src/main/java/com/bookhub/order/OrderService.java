package com.bookhub.order;

import com.bookhub.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<OrderDTO> findAllOrders() {
        List<Order> orders = orderRepository.findAll();
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
        // Gọi phương thức Repository đã sửa lỗi
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
        dto.setPriceAtDate(detail.getPrice_date());
        dto.setPriceAtDateFormatted(String.format("%,dđ", detail.getPrice_date()).replace(",", "."));

        // Cần truy cập Product Entity để lấy tên và tác giả sản phẩm
        if (detail.getProduct() != null) {
            // dto.setProductName(detail.getProduct().getTitle());
            // dto.setProductAuthor(detail.getProduct().getAuthor());
        }
        return dto;
    }
}