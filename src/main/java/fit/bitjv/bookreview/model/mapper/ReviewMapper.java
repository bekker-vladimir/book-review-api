package fit.bitjv.bookreview.model.mapper;

import fit.bitjv.bookreview.model.dto.response.ReviewResponseDto;
import fit.bitjv.bookreview.model.entity.Review;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class ReviewMapper {
    public ReviewResponseDto toDto(Review reviewEntity) {
        return new ReviewResponseDto(
                reviewEntity.getId(),
                reviewEntity.getRating(),
                reviewEntity.getComment(),
                reviewEntity.getBook().getId(),
                reviewEntity.getBook().getTitle(),
                reviewEntity.getUser().getUsername(),
                reviewEntity.getCreatedAt() != null ?
                        reviewEntity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm"))
                        : null
        );
    }
}
