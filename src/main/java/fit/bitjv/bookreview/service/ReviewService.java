package fit.bitjv.bookreview.service;

import fit.bitjv.bookreview.exception.ResourceNotFoundException;
import fit.bitjv.bookreview.exception.UnauthorizedAccessException;
import fit.bitjv.bookreview.model.mapper.ReviewMapper;
import fit.bitjv.bookreview.model.dto.request.ReviewRequestDto;
import fit.bitjv.bookreview.model.dto.response.ReviewResponseDto;
import fit.bitjv.bookreview.model.entity.Book;
import fit.bitjv.bookreview.model.entity.Review;
import fit.bitjv.bookreview.model.entity.Role;
import fit.bitjv.bookreview.model.entity.User;
import fit.bitjv.bookreview.repository.BookRepository;
import fit.bitjv.bookreview.repository.ReviewRepository;
import fit.bitjv.bookreview.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookRepository bookRepository,
            UserRepository userRepository,
            ReviewMapper reviewMapper
    ) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.reviewMapper = reviewMapper;
    }

    @Transactional
    public ReviewResponseDto createReviewForBook(ReviewRequestDto reviewRequestDto, Long bookId, String authenticatedUsername) {
        User user = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new UnauthorizedAccessException("Session expired"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", bookId));

        Review review = Review.builder()
                .rating(reviewRequestDto.getRating())
                .comment(reviewRequestDto.getComment())
                .book(book)
                .user(user)
                .build();

        Review saved = reviewRepository.save(review);

        reviewRepository.recalculateBookStats(bookId);

        return reviewMapper.toDto(saved);
    }

    public Page<ReviewResponseDto> getReviewsByBook(Long bookId, Pageable pageable) {
        return reviewRepository.findByBookId(bookId, pageable)
                .map(reviewMapper::toDto);
    }

    public List<ReviewResponseDto> getRecent(int count) {
        return reviewRepository.getRecent(count)
                .stream()
                .map(reviewMapper::toDto)
                .toList();
    }

    @Transactional
    public void deleteReview(Long reviewId, String authenticatedUsername) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        User user = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new UnauthorizedAccessException("Session expired"));

        boolean isOwner = review.getUser().getUsername().equals(authenticatedUsername);
        boolean isModerator = user.getRole() == Role.ADMIN || user.getRole() == Role.MODERATOR;

        if (!isOwner && !isModerator) {
            throw new UnauthorizedAccessException("You are not allowed to delete this review");
        }

        reviewRepository.delete(review);
    }
}