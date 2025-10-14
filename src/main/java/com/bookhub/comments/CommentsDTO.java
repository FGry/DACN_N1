package com.bookhub.comments;

import lombok.*;

import java.time.LocalDate;

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
}
