package fit.bitjv.bookreview.model.mapper;

import fit.bitjv.bookreview.model.dto.response.BookResponseDto;
import fit.bitjv.bookreview.model.entity.Author;
import fit.bitjv.bookreview.model.entity.Book;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
@AllArgsConstructor
public class BookMapper {
    private final AuthorMapper authorMapper;

    public BookResponseDto toDto(Book bookEntity) {
        String coverUrl = null;
        if (bookEntity.getCoverPath() != null) {
            String[] parts = bookEntity.getCoverPath().replace("\\", "/").split("/");
            String filename = parts[parts.length - 1];
            coverUrl = "/books/covers/" + filename;
        }

        return new BookResponseDto(
                bookEntity.getId(),
                bookEntity.getTitle(),
                bookEntity.getGenre(),
                bookEntity.getDescription(),
                bookEntity.getPublicationDate(),
                bookEntity.getAvgRating(),
                bookEntity.getReviewCount(),
                bookEntity.getStatus(),
                coverUrl,
                bookEntity.getAuthors().stream()
                        .sorted(Comparator.comparing(Author::getId))
                        .map(authorMapper::toDto)
                        .toList());
    }
}
