package fit.bitjv.bookreview.repository;

import fit.bitjv.bookreview.model.entity.Complaint;
import fit.bitjv.bookreview.model.entity.Review;
import fit.bitjv.bookreview.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    boolean existsByReviewAndUser(@Param("review") Review review, @Param("user") User user);
}
