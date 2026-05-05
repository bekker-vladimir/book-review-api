package fit.bitjv.bookreview.model.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Entity
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_review")
    private Long id;

    @Setter
    private int rating;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Setter
    @Column(length = 5000)
    private String comment;

    @Setter
    @ManyToOne
    @JoinColumn(name = "id_book")
    private Book book;

    @Setter
    @ManyToOne
    @JoinColumn(name = "id_user")
    private User user;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<Complaint> complaints = new HashSet<>();

    @Builder
    public Review(int rating, LocalDateTime createdAt, String comment, Book book, User user) {
        this.rating = rating;
        this.createdAt = createdAt;
        this.comment = comment;
        this.book = book;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Review review)) return false;
        return Objects.equals(user, review.user) &&
                Objects.equals(book, review.book);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, book);
    }
}

