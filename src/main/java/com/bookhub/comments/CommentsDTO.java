package com.bookhub.comments;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentsDTO {
    private Integer id;
    private String messages;
    private Integer rate;
    private LocalDate date;
    private Integer userId;
    private Integer productId;

    private String productTitle;
    private String productCode;
    private String userName;
    private String status;
    private String reply;
    private LocalDateTime replyDate;
    private String productImageUrl;
    private Boolean purchaseVerified;
}