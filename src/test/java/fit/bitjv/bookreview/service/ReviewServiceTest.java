package fit.bitjv.bookreview.service;

import fit.bitjv.bookreview.exception.ResourceNotFoundException;
import fit.bitjv.bookreview.exception.UnauthorizedAccessException;
import fit.bitjv.bookreview.model.dto.request.ReviewRequestDto;
import fit.bitjv.bookreview.model.dto.response.ReviewResponseDto;
import fit.bitjv.bookreview.model.entity.Book;
import fit.bitjv.bookreview.model.entity.Review;
import fit.bitjv.bookreview.model.entity.Role;
import fit.bitjv.bookreview.model.entity.User;
import fit.bitjv.bookreview.model.mapper.ReviewMapper;
import fit.bitjv.bookreview.repository.BookRepository;
import fit.bitjv.bookreview.repository.ReviewRepository;
import fit.bitjv.bookreview.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock BookRepository bookRepository;
    @Mock UserRepository userRepository;
    @Mock ReviewMapper reviewMapper;

    @InjectMocks ReviewService reviewService;

    // createReview

    @Test
    void createReview_success() {
        User user = buildUser("alice", Role.USER);
        Book book = new Book();
        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setRating(5);
        dto.setComment("Great!");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        Review saved = Review.builder().rating(5).comment("Great!").user(user).book(book).build();
        when(reviewRepository.save(any())).thenReturn(saved);
        when(reviewMapper.toDto(saved)).thenReturn(new ReviewResponseDto());

        reviewService.createReviewForBook(dto, 1L, "alice");

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_throwsWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReviewForBook(new ReviewRequestDto(), 1L, "ghost"))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void createReview_throwsWhenBookNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(buildUser("alice", Role.USER)));
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReviewForBook(new ReviewRequestDto(), 99L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // deleteReview

    @Test
    void deleteReview_ownerCanDelete() {
        User alice = buildUser("alice", Role.USER);
        Review review = buildReview(alice);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        reviewService.deleteReview(1L, "alice");

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_moderatorCanDelete() {
        User owner = buildUser("bob", Role.USER);
        User mod = buildUser("mod", Role.MODERATOR);
        Review review = buildReview(owner);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findByUsername("mod")).thenReturn(Optional.of(mod));

        reviewService.deleteReview(1L, "mod");

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_adminCanDelete() {
        User owner = buildUser("bob", Role.USER);
        User admin = buildUser("admin", Role.ADMIN);
        Review review = buildReview(owner);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        reviewService.deleteReview(1L, "admin");

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_strangerCannotDelete() {
        User owner = buildUser("bob", Role.USER);
        User stranger = buildUser("eve", Role.USER);
        Review review = buildReview(owner);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findByUsername("eve")).thenReturn(Optional.of(stranger));

        assertThatThrownBy(() -> reviewService.deleteReview(1L, "eve"))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void deleteReview_throwsWhenReviewNotFound() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(99L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteReview_throwsWhenUserNotFound() {
        Review review = buildReview(buildUser("bob", Role.USER));

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(1L, "ghost"))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    // helpers

    private User buildUser(String username, Role role) {
        User u = new User(username, username + "@test.com", "pw");
        u.setRole(role);
        return u;
    }

    private Review buildReview(User owner) {
        Review review = Review.builder().user(owner).build();
        ReflectionTestUtils.setField(review, "id", 1L);
        return review;
    }
}