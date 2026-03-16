package fit.bitjv.bookreview.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookRequestDto {

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @NotBlank(message = "Genre cannot be blank")
    private String genre;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private String publicationDate;

    @NotEmpty(message = "Book must have at least one author")
    private Set<String> authorNames;
}
