package fit.bitjv.bookreview.model.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDto {
    private Long id;
    private int rating;
    private String comment;
    private String username;
    private String createdAt;
}