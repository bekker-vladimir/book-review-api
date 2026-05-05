package fit.bitjv.bookreview.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_complaint")
    private Long id;

    @Column(length = 100)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Complaint(String reason, User user, Review review) {
        this.reason = reason;
        this.user = user;
        this.review = review;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Complaint complaint)) return false;
        return Objects.equals(review, complaint.review) &&
                Objects.equals(user, complaint.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(review, user);
    }
}