package fit.bitjv.bookreview.repository;

import fit.bitjv.bookreview.model.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("SELECT r FROM Review r JOIN FETCH r.user JOIN FETCH r.book WHERE r.book.id = :bookId")
    Page<Review> findByBookIdWithUserAndBook(@Param("bookId") Long bookId, Pageable pageable);

    @Modifying
    @Query("UPDATE Book b SET " +
            "b.avgRating = (SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId), " +
            "b.reviewCount = (SELECT COUNT(r) FROM Review r WHERE r.book.id = :bookId) " +
            "WHERE b.id = :bookId")
    void recalculateBookStats(@Param("bookId") Long bookId);

    @Query("SELECT r FROM Review r JOIN FETCH r.user JOIN FETCH r.book WHERE r.book.status = 'APPROVED' " +
            "ORDER BY r.createdAt DESC LIMIT :count")
    List<Review> findRecent(@Param("count") int count);
}
