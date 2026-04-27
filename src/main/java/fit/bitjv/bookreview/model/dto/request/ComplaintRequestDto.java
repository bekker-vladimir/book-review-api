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
public class ComplaintRequestDto {

    @NotBlank(message = "Reason cannot be blank")
    @Size(min = 4, max = 500, message = "Reason must be between 4 and 500 characters")
    private String reason;
}