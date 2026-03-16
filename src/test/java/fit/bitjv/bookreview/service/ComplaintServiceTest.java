package fit.bitjv.bookreview.service;

import fit.bitjv.bookreview.exception.ResourceAlreadyExistsException;
import fit.bitjv.bookreview.exception.ResourceNotFoundException;
import fit.bitjv.bookreview.exception.UnauthorizedAccessException;
import fit.bitjv.bookreview.model.dto.request.ComplaintRequestDto;
import fit.bitjv.bookreview.model.entity.Complaint;
import fit.bitjv.bookreview.model.entity.Review;
import fit.bitjv.bookreview.model.entity.Role;
import fit.bitjv.bookreview.model.entity.User;
import fit.bitjv.bookreview.model.mapper.ComplaintMapper;
import fit.bitjv.bookreview.repository.ComplaintRepository;
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
class ComplaintServiceTest {

    @Mock ComplaintRepository complaintRepository;
    @Mock UserRepository userRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock ComplaintMapper complaintMapper;

    @InjectMocks ComplaintService complaintService;

    // createComplaint

    @Test
    void createComplaint_success() {
        User user = buildUser("alice");
        Review review = buildReview(1L);
        ComplaintRequestDto dto = new ComplaintRequestDto();
        dto.setReason("Spam");

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(complaintRepository.existsByReviewAndUser(review, user)).thenReturn(false);
        Complaint saved = new Complaint("Spam", user, review);
        when(complaintRepository.save(any())).thenReturn(saved);
        when(complaintMapper.toDto(saved)).thenReturn(null);

        complaintService.createComplaintForReview(dto, 1L, "alice");

        verify(complaintRepository).save(any(Complaint.class));
    }

    @Test
    void createComplaint_throwsWhenAlreadyComplained() {
        User user = buildUser("alice");
        Review review = buildReview(1L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(complaintRepository.existsByReviewAndUser(review, user)).thenReturn(true);

        assertThatThrownBy(() ->
                complaintService.createComplaintForReview(new ComplaintRequestDto(), 1L, "alice"))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void createComplaint_throwsWhenReviewNotFound() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                complaintService.createComplaintForReview(new ComplaintRequestDto(), 99L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createComplaint_throwsWhenUserNotFound() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(buildReview(1L)));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                complaintService.createComplaintForReview(new ComplaintRequestDto(), 1L, "ghost"))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    // deleteById

    @Test
    void deleteById_throwsWhenNotFound() {
        when(complaintRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> complaintService.deleteById(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteById_deletesWhenExists() {
        when(complaintRepository.existsById(5L)).thenReturn(true);

        complaintService.deleteById(5L);

        verify(complaintRepository).deleteById(5L);
    }

    // helpers

    private User buildUser(String username) {
        User u = new User(username, username + "@test.com", "pw");
        u.setRole(Role.USER);
        return u;
    }

    private Review buildReview(Long id) {
        Review review = new Review();
        ReflectionTestUtils.setField(review, "id", id);
        return review;
    }
}