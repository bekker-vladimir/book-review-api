package fit.bitjv.bookreview.controller;

import fit.bitjv.bookreview.model.dto.request.BookRequestDto;
import fit.bitjv.bookreview.model.dto.request.ReviewRequestDto;
import fit.bitjv.bookreview.model.dto.response.BookResponseDto;
import fit.bitjv.bookreview.model.dto.response.ReviewResponseDto;
import fit.bitjv.bookreview.model.entity.BookStatus;
import fit.bitjv.bookreview.service.BookService;
import fit.bitjv.bookreview.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/books")
@Tag(name = "Book Controller", description = "CRUD operations for books")
public class BookController {
    private final BookService bookService;
    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Get all approved books (paginated)")
    public ResponseEntity<Page<BookResponseDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(bookService.getAllPaged(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search books by title or author (paginated)")
    public ResponseEntity<Page<BookResponseDto>> searchBooks(
            @RequestParam String query, Pageable pageable) {
        return ResponseEntity.ok(bookService.searchBooksPaged(query, pageable));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get all pending books (Admin/Moderator only)")
    public ResponseEntity<List<BookResponseDto>> getPending() {
        return ResponseEntity.ok(bookService.getAllByStatus(BookStatus.PENDING));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by id")
    public ResponseEntity<BookResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete book by id")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @Operation(summary = "Create new book")
    public ResponseEntity<BookResponseDto> createBook(@Valid @RequestBody BookRequestDto bookRequestDto) {
        BookResponseDto saved = bookService.createBook(bookRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{bookId}/reviews")
    @Operation(summary = "Get reviews for a specific book")
    public ResponseEntity<Page<ReviewResponseDto>> getReviewsByBook(
            @PathVariable Long bookId,
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByBook(bookId, pageable));
    }

    @PostMapping("/{bookId}/reviews")
    @Operation(summary = "Create new review by a user")
    public ResponseEntity<ReviewResponseDto> createReviewForBook(
            @Valid @RequestBody ReviewRequestDto reviewRequestDto,
            @PathVariable Long bookId,
            Authentication authentication
    ) {
        ReviewResponseDto saved = reviewService.createReviewForBook(reviewRequestDto, bookId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/{id}/cover")
    @Operation(summary = "Upload a cover for a book")
    public ResponseEntity<Map<String, String>> uploadCover(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String coverPath = bookService.uploadCover(id, file);
        return ResponseEntity.ok(Map.of("message", "Cover uploaded successfully", "coverPath", coverPath));
    }

    @GetMapping("/covers/{filename:.+}")
    @Operation(summary = "Serve a book cover image")
    public ResponseEntity<Resource> serveCoverFile(@PathVariable String filename) {
        return bookService.getCoverResponse(filename);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change the status of a book (Admin/Moderator only)")
    public ResponseEntity<Map<String, String>> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        BookStatus status = bookService.parseStatus(body.get("status"));
        bookService.changeStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
    }
}