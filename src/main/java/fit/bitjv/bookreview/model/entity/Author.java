package fit.bitjv.bookreview.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Entity
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_author")
    private Long id;

    @Getter
    @Column(name = "full_name")
    private String fullName;

    @ManyToMany(mappedBy = "authors")
    private Set<Book> books;
}
