package fit.bitjv.bookreview.repository;

import fit.bitjv.bookreview.model.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByFullName(String fullName);
    List<Author> findAllByFullNameIn(Collection<String> fullNames);
}
