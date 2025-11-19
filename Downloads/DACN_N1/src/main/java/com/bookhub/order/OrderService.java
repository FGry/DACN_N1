package com.bookhub.order;

import com.bookhub.product.Product;
import com.bookhub.product.ProductRepository;
import com.bookhub.user.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderAccessTokenRepository tokenRepository;
    private final ProductRepository productRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional
    public OrderDTO placeGuestOrder(GuestCheckoutDTO checkoutDTO) {
        Order newOrder = Order.builder()
                .address(checkoutDTO.getAddress())
                .phone(checkoutDTO.getPhone())
                .status_order("SUCCESS")
                .date(LocalDate.now())
                .payment_method(checkoutDTO.getPaymentMethod())
                .user(new User(1))
                .orderDetails(new ArrayList<>())
                .build();

        long finalTotal = 0;

        try {
            if (checkoutDTO.getItemsJson() != null) {
                ObjectMapper mapper = new ObjectMapper();
                List<OrderDTO.OrderItemDTO> cartItems = mapper.readValue(checkoutDTO.getItemsJson(), new TypeReference<List<OrderDTO.OrderItemDTO>>(){});

                for (OrderDTO.OrderItemDTO item : cartItems) {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new RuntimeException("SP không tồn tại: " + item.getProductId()));

                    long originalPrice = product.getPrice();
                    long finalPrice = originalPrice;
                    if (product.getDiscount() != null && product.getDiscount() > 0) {
                        finalPrice = originalPrice - (originalPrice * product.getDiscount() / 100);
                    }

                    long itemTotal = finalPrice * item.getQuantity();
                    finalTotal += itemTotal;

                    OrderDetail detail = OrderDetail.builder()
                            .product(product)
                            .order(newOrder)
                            .quantity((long) item.getQuantity())
                            .price_date(finalPrice)
                            .total(itemTotal)
                            .discount(product.getDiscount())
                            .build();
                    newOrder.getOrderDetails().add(detail);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi: " + e.getMessage());
        }

        newOrder.setTotal(finalTotal);
        newOrder = orderRepository.save(newOrder);

        String tokenValue = UUID.randomUUID().toString().replace("-", "");
        OrderAccessToken token = OrderAccessToken.builder()
                .order(newOrder)
                .accessToken(tokenValue)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .isUsed(false)
                .build();
        tokenRepository.save(token);

        OrderDTO dto = mapToDetailDTO(newOrder);
        dto.setGuestAccessToken(tokenValue);
        return dto;
    }

    @Transactional
    public OrderDTO getOrderByAccessToken(String token) {
        Optional<OrderAccessToken> opt = tokenRepository.findByAccessToken(token);
        if (opt.isEmpty()) throw new RuntimeException("Mã không hợp lệ.");
        OrderAccessToken access = opt.get();

        if (access.getExpiresAt().isBefore(LocalDateTime.now())) throw new RuntimeException("Mã hết hạn.");

        if (!access.isUsed()) { access.setUsed(true); tokenRepository.save(access); }

        Order order = orderRepository.findByIdWithDetails(access.getOrder().getId_order())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn."));
        return mapToDetailDTO(order);
    }

    // --- ADMIN METHODS ---
    @Transactional(readOnly = true) public List<OrderDTO> searchOrders(String s) { return orderRepository.searchOrders(s).stream().map(this::mapToDTO).collect(Collectors.toList()); }
    @Transactional(readOnly = true) public List<OrderDTO> filterOrders(String s) { if(s==null||s.isEmpty()) return findAllOrders(); return orderRepository.findByStatus_orderIgnoreCase(s).stream().map(this::mapToDTO).collect(Collectors.toList()); }
    public List<OrderDTO> findAllOrders() { return orderRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList()); }
    public OrderDTO findOrderById(Integer id) { return mapToDetailDTO(orderRepository.findByIdWithDetails(id).orElseThrow()); }
    @Transactional public void updateOrderStatus(Integer id, String s) { Order o=orderRepository.findById(id).orElseThrow(); o.setStatus_order(s); orderRepository.save(o); }
    @Transactional public void cancelOrder(Integer id) { Order o=orderRepository.findById(id).orElseThrow(); if("DELIVERED".equalsIgnoreCase(o.getStatus_order())||"CANCELLED".equalsIgnoreCase(o.getStatus_order())) throw new RuntimeException("Err"); o.setStatus_order("CANCELLED"); orderRepository.save(o); }

    // --- MAPPING ---
    private OrderDTO mapToDTO(Order entity) {
        OrderDTO dto = new OrderDTO();
        dto.setIdOrder(entity.getId_order());
        dto.setOrderCode("#DH" + entity.getId_order());
        dto.setCustomerUsername(entity.getUser() != null && entity.getUser().getIdUser() != 1 ? entity.getUser().getUsername() : "Khách vãng lai");
        dto.setUserId(entity.getUser() != null ? entity.getUser().getIdUser() : null);
        dto.setCustomerPhone(entity.getPhone());
        dto.setTotalAmount(entity.getTotal());
        dto.setTotalAmountFormatted(String.format("%,dđ", entity.getTotal()).replace(",", "."));
        dto.setStatus(entity.getStatus_order());
        dto.setDate(entity.getDate());
        if(entity.getDate() != null) dto.setDateFormatted(entity.getDate().format(DATE_FORMATTER));
        if (entity.getOrderDetails() != null) dto.setTotalProducts(entity.getOrderDetails().size());
        return dto;
    }

    private OrderDTO mapToDetailDTO(Order entity) {
        OrderDTO dto = mapToDTO(entity);
        dto.setAddress(entity.getAddress());
        dto.setPaymentMethod(entity.getPayment_method());
        dto.setNote(entity.getNote());
        if (entity.getOrderDetails() != null) {
            dto.setProductDetails(entity.getOrderDetails().stream().map(this::mapDetailToDTO).collect(Collectors.toList()));
        } else {
            dto.setProductDetails(new ArrayList<>());
        }
        return dto;
    }

    private OrderDTO.OrderDetailDTO mapDetailToDTO(OrderDetail detail) {
        OrderDTO.OrderDetailDTO dto = new OrderDTO.OrderDetailDTO();

        // Lấy dữ liệu
        long qty = detail.getQuantity() != null ? detail.getQuantity() : 0;
        long price = detail.getPrice_date() != null ? detail.getPrice_date() : 0;
        // Tính tổng tiền dòng này
        long totalLinePrice = qty * price;

        dto.setQuantity((int) qty);
        dto.setPriceAtDate(price);

        // Format Đơn giá
        dto.setPriceAtDateFormatted(String.format("%,dđ", price).replace(",", "."));

        // ⭐ GÁN GIÁ TRỊ THÀNH TIỀN ĐÃ FORMAT ⭐
        dto.setTotalPriceFormatted(String.format("%,dđ", totalLinePrice).replace(",", "."));

        if (detail.getProduct() != null) {
            dto.setProductName(detail.getProduct().getTitle());
            dto.setProductAuthor(detail.getProduct().getAuthor());
        }
        return dto;
    }
}