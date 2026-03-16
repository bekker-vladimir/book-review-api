package fit.bitjv.bookreview.model.mapper;

import fit.bitjv.bookreview.model.dto.response.ComplaintResponseDto;
import fit.bitjv.bookreview.model.entity.Complaint;
import fit.bitjv.bookreview.model.entity.Review;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class ComplaintMapper {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ComplaintResponseDto toDto(Complaint complaintEntity) {
        Review review = complaintEntity.getReview();
        return new ComplaintResponseDto(
                complaintEntity.getId(),
                complaintEntity.getReason(),
                complaintEntity.getUser().getUsername(),
                review != null ? review.getId() : null,
                review != null ? review.getComment() : null,
                review != null && review.getUser() != null ? review.getUser().getUsername() : null,
                review != null && review.getCreatedAt() != null
                        ? review.getCreatedAt().format(FORMATTER) : null
        );
    }
}