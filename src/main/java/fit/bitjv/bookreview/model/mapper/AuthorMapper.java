package fit.bitjv.bookreview.model.mapper;

import fit.bitjv.bookreview.model.dto.response.AuthorResponseDto;
import fit.bitjv.bookreview.model.entity.Author;
import org.springframework.stereotype.Component;

@Component
public class AuthorMapper {
    public AuthorResponseDto toDto(Author authorEntity){
        return new AuthorResponseDto(authorEntity.getId(), authorEntity.getFullName());
    }
}
