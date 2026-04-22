package fit.bitjv.bookreview.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import fit.bitjv.bookreview.model.entity.BookStatus;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookResponseDto {
    private Long id;
    private String title;
    private String genre;
    private String description;
    private String publicationDate;
    private Double avgRating;
    private int reviewCount;
    private String approvedAt;
    private BookStatus status;
    private String coverUrl;
    private List<AuthorResponseDto> authors;
}