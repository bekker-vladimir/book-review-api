package fit.bitjv.bookreview.repository;

import fit.bitjv.bookreview.model.entity.Book;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import fit.bitjv.bookreview.model.entity.BookStatus;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @EntityGraph(attributePaths = {"authors"})
    @Query("SELECT b FROM Book b WHERE b.status = :status")
    List<Book> findAllWithAuthorsByStatus(@Param("status") BookStatus status);

    // @EntityGraph + Pageable causes HHH90003004 (in-memory pagination)
    // use JOIN FETCH with a separate countQuery instead
    @Query(value = "SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.authors WHERE b.status = :status",
            countQuery = "SELECT COUNT(DISTINCT b) FROM Book b WHERE b.status = :status")
    Page<Book> findAllWithAuthorsByStatus(@Param("status") BookStatus status, Pageable pageable);

    @Query("SELECT b FROM Book b JOIN b.authors a WHERE " +
            "(LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "b.status = :status")
    List<Book> searchByTitleOrAuthorAndStatus(@Param("query") String query, @Param("status") BookStatus status);

    @Query(value = "SELECT DISTINCT b FROM Book b JOIN b.authors a WHERE " +
            "(LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "b.status = :status",
            countQuery = "SELECT COUNT(DISTINCT b) FROM Book b JOIN b.authors a WHERE " +
                    "(LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                    "LOWER(a.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
                    "b.status = :status")
    Page<Book> searchByTitleOrAuthorAndStatus(@Param("query") String query, @Param("status") BookStatus status, Pageable pageable);
}