package fit.bitjv.bookreview.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_book")
    private Long id;

    private String title;

    private String genre;

    @Column(length = 2000)
    private String description;

    @Column(name = "publication_date")
    private String publicationDate;

    @Enumerated(EnumType.STRING)
    private BookStatus status = BookStatus.PENDING;

    @Column(name = "cover_path")
    private String coverPath;

    @ManyToMany
    @JoinTable(
            name = "book_author",
            joinColumns = @JoinColumn(name = "id_book"),
            inverseJoinColumns = @JoinColumn(name = "id_author")
    )
    private Set<Author> authors;

    @OneToMany(mappedBy = "book", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<Review> reviews;
}