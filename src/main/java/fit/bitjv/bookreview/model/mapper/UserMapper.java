package fit.bitjv.bookreview.model.mapper;

import fit.bitjv.bookreview.model.dto.response.UserResponseDto;
import fit.bitjv.bookreview.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponseDto toDto(User userEntity) {
        return new UserResponseDto(
                userEntity.getId(),
                userEntity.getUsername()
        );
    }
}
