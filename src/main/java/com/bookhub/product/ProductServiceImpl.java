package com.bookhub.product;

import com.bookhub.comments.Comments;
import com.bookhub.comments.CommentsDTO;
import com.bookhub.comments.CommentsRepository;
import com.bookhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale; // Cần import cho String.format
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CommentsRepository commentsRepository;
    private final UserRepository userRepository; // Giả định

    @Override
    public ProductDTO getProductDetail(Integer productId) {

        Optional<Product> productOpt = productRepository.findById(productId);

        if (productOpt.isEmpty()) {
            return null;
        }

        Product product = productOpt.get();
        List<Comments> comments = commentsRepository.findByProduct_IdProducts(productId);
        Double averageRating = comments.stream()
                .mapToInt(Comments::getRate)
                .average()
                .orElse(0.0);
        Long totalReviews = (long) comments.size();

        List<CommentsDTO> commentsDTOs = comments.stream()
                .map(this::convertToCommentsDTO)
                .collect(Collectors.toList());

        return ProductDTO.builder()
                .idProducts(product.getIdProducts())
                .title(product.getTitle())
                .price(product.getPrice())
                .author(product.getAuthor())
                .publisher(product.getPublisher())
                .publicationYear(product.getPublicationYear())
                .pages(product.getPages())
                .stockQuantity(product.getStockQuantity())
                .language(product.getLanguage())
                .discount(product.getDiscount())
                .categoryName(product.getCategories().isEmpty() ? "Chưa phân loại" : product.getCategories().get(0).getName())
                .images(product.getImages().stream().map(this::convertToImageProductDTO).collect(Collectors.toList()))
                .averageRating(Double.parseDouble(String.format(Locale.US, "%.1f", averageRating)))
                .totalReviews(totalReviews)
                .comments(commentsDTOs)
                .detailDescription("Sách giáo khoa Toán học lớp 12 được biên soạn theo chương trình giáo dục phổ thông mới...")
                .build();
    }


    private ImageProductDTO convertToImageProductDTO(ImageProduct image) {
        return ImageProductDTO.builder()
                .id_image_product(image.getId_image_product())
                .image_link(image.getImage_link())
                .build();
    }

    private CommentsDTO convertToCommentsDTO(Comments comment) {
        return CommentsDTO.builder()
                .id(comment.getIdComment())
                .date(comment.getDate())
                .messages(comment.getMessages())
                .rate(comment.getRate())
                .userId(comment.getUser() != null ? comment.getUser().getIdUser() : null)
                .build();
    }
}