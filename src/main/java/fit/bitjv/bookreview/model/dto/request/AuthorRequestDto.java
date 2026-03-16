package fit.bitjv.bookreview.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthorRequestDto {

    @NotBlank(message = "Author full name cannot be blank")
    @Size(min = 2, max = 100, message = "Author full name must be between 2 and 100 characters")
    private String fullName;
}