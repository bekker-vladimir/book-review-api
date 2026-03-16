package fit.bitjv.bookreview.service;

import fit.bitjv.bookreview.exception.ResourceNotFoundException;
import fit.bitjv.bookreview.exception.UnauthorizedAccessException;
import fit.bitjv.bookreview.exception.ResourceAlreadyExistsException;
import fit.bitjv.bookreview.model.dto.request.ComplaintRequestDto;
import fit.bitjv.bookreview.model.dto.response.ComplaintResponseDto;
import fit.bitjv.bookreview.model.entity.Complaint;
import fit.bitjv.bookreview.model.entity.Review;
import fit.bitjv.bookreview.model.entity.User;
import fit.bitjv.bookreview.model.mapper.ComplaintMapper;
import fit.bitjv.bookreview.repository.ComplaintRepository;
import fit.bitjv.bookreview.repository.ReviewRepository;
import fit.bitjv.bookreview.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ComplaintService {
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ComplaintMapper complaintMapper;

    public ComplaintService(ComplaintRepository complaintRepository, UserRepository userRepository, ReviewRepository reviewRepository, ComplaintMapper complaintMapper) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.complaintMapper = complaintMapper;
    }

    @Transactional
    public ComplaintResponseDto createComplaintForReview(ComplaintRequestDto complaintDto, Long reviewId, String authenticatedUsername) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        User user = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new UnauthorizedAccessException("Session expired"));

        boolean hasComplained = complaintRepository.existsByReviewAndUser(review, user);
        if (hasComplained) {
            throw new ResourceAlreadyExistsException("User has already complained about this review");
        }

        Complaint complaint = new Complaint(complaintDto.getReason(), user, review);
        complaintRepository.save(complaint);

        return complaintMapper.toDto(complaint);
    }

    public List<ComplaintResponseDto> getAll() {
        return complaintRepository.findAll()
                .stream()
                .map(complaintMapper::toDto)
                .toList();
    }

    @Cacheable(value = "complaints", key = "#id")
    public Optional<Complaint> getById(Long id) {
        return complaintRepository.findById(id);
    }

    @CacheEvict(value = "complaints", key = "#id")
    public void deleteById(Long id) {
        if (!complaintRepository.existsById(id)) {
            throw new ResourceNotFoundException("Complaint", "id", id);
        }
        complaintRepository.deleteById(id);
    }
}