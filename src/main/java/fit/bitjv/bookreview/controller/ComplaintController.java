package fit.bitjv.bookreview.controller;

import fit.bitjv.bookreview.model.dto.request.ComplaintRequestDto;
import fit.bitjv.bookreview.model.dto.response.ComplaintResponseDto;
import fit.bitjv.bookreview.service.ComplaintService;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/complaints")
@Tag(name = "Complaint Controller", description = "CRUD operations for complaints")
public class ComplaintController {
    private final ComplaintService complaintService;

    @PostMapping("/review/{reviewId}")
    @Operation(summary = "Create new complaint for a review")
    public ResponseEntity<ComplaintResponseDto> createComplaintForReview(
            @Valid @RequestBody ComplaintRequestDto complaintRequestDto,
            @PathVariable Long reviewId,
            Authentication authentication
    ) {
        ComplaintResponseDto complaint = complaintService.createComplaintForReview(
                complaintRequestDto, reviewId, authentication.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(complaint);
    }

    @GetMapping
    @Operation(summary = "Get all complaints (Admin/Moderator only)")
    public ResponseEntity<List<ComplaintResponseDto>> getAll() {
        return ResponseEntity.ok(complaintService.getAll());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dismiss a complaint (Admin/Moderator only)")
    public ResponseEntity<Void> dismiss(@PathVariable Long id) {
        complaintService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}