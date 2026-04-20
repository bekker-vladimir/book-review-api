package fit.bitjv.bookreview.controller;

import fit.bitjv.bookreview.model.dto.response.ReviewResponseDto;
import fit.bitjv.bookreview.service.ReviewService;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@Tag(name = "Review Controller", description = "CRUD operations for reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/recent")
    @Operation(summary = "Get a specified number of recent reviews")
    public ResponseEntity<List<ReviewResponseDto>> getRecent(@RequestParam("count") int count){
        return ResponseEntity.ok(reviewService.getRecent(count));
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete a review (owner or moderator/admin)")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication
    ) {
        reviewService.deleteReview(reviewId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}