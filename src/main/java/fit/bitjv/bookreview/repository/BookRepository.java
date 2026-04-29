package fit.bitjv.bookreview.repository;

import fit.bitjv.bookreview.model.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import fit.bitjv.bookreview.model.entity.BookStatus;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.authors WHERE b.id = :id")
    Optional<Book> findByIdWithAuthors(@Param("id") Long id);

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.authors WHERE b.status = :status")
    List<Book> findAllWithAuthorsByStatus(@Param("status") BookStatus status);

    // -------------------------------------------------------------------------
    // Paginated queries — Two-step approach (ID-first pagination):
    //   Step 1. Fetch a page of IDs (without JOIN — clean LIMIT/OFFSET in SQL).
    //   Step 2. Fetch full entities by IDs using JOIN FETCH (see BookService).
    //
    // Why JOIN FETCH + Pageable fails:
    //   JOINing a collection multiplies the rows (book × N authors).
    //   Hibernate cannot apply SQL LIMIT to this multiplied result set,
    //   so it fetches ALL rows into memory and paginates them there —
    //   causing the HHH90003004 warning (In-memory pagination).
    //   A separate countQuery does not fix this underlying issue.
    // -------------------------------------------------------------------------

    /**
     * Step 1: Fetch a page of book IDs by status.
     * Simple query without JOINs => Hibernate applies LIMIT/OFFSET at the SQL level.
     */
    @Query(value = "SELECT b.id FROM Book b WHERE b.status = :status",
            countQuery = "SELECT COUNT(b) FROM Book b WHERE b.status = :status")
    Page<Long> findIdsByStatus(@Param("status") BookStatus status, Pageable pageable);

    @Query(value = "SELECT b.id FROM Book b WHERE " +
            "(LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "EXISTS (SELECT 1 FROM b.authors a WHERE LOWER(a.fullName) LIKE LOWER(CONCAT('%', :query, '%')))) AND " +
            "b.status = :status",
            countQuery = "SELECT COUNT(b) FROM Book b WHERE " +
                    "(LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                    "EXISTS (SELECT 1 FROM b.authors a WHERE LOWER(a.fullName) LIKE LOWER(CONCAT('%', :query, '%')))) AND " +
                    "b.status = :status")
    Page<Long> findIdsByTitleOrAuthorAndStatus(@Param("query") String query,
                                               @Param("status") BookStatus status,
                                               Pageable pageable);

    /**
     * Even without Pageable, LIMIT inside JPQL with JOIN FETCH causes the same issue.
     * Therefore, same solution is applied.
     */
    @Query("SELECT b.id FROM Book b WHERE b.reviewCount >= 3 ORDER BY b.avgRating DESC LIMIT :count")
    List<Long> findTopRatedIds(@Param("count") int count);

    @Query("SELECT b.id FROM Book b WHERE b.status = :status ORDER BY b.approvedAt DESC LIMIT :count")
    List<Long> findRecentlyAddedIds(@Param("status") BookStatus status, @Param("count") int count);

    /**
     * Step 2: Fetch books by ID list in a single query using JOIN FETCH.
     * Without Pageable parameter - no row multiplication issues, no HHH90003004.
     */
    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.authors WHERE b.id IN :ids")
    List<Book> findAllWithAuthorsByIds(@Param("ids") List<Long> ids);
}