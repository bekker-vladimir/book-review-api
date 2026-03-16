package fit.bitjv.bookreview.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintResponseDto {
    private Long id;
    private String reason;
    private String complaintAuthor;
    private Long reviewId;
    private String reviewContent;
    private String reviewAuthor;
    private String date;
}