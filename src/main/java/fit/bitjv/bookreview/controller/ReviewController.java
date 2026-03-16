package fit.bitjv.bookreview.controller;

import fit.bitjv.bookreview.model.dto.request.ReviewRequestDto;
import fit.bitjv.bookreview.model.dto.response.ReviewResponseDto;
import fit.bitjv.bookreview.service.ReviewService;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/reviews")
@Tag(name = "Review Controller", description = "CRUD operations for reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/book/{bookId}")
    @Operation(summary = "Create new review by a user")
    public ResponseEntity<ReviewResponseDto> createReview(
            @Valid @RequestBody ReviewRequestDto reviewResponseDto,
            @PathVariable Long bookId,
            Authentication authentication
    ) {
        ReviewResponseDto saved = reviewService.createReviewForBook(reviewResponseDto, bookId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
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