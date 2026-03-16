package fit.bitjv.bookreview.service;

import fit.bitjv.bookreview.model.dto.response.UserResponseDto;
import fit.bitjv.bookreview.model.mapper.UserMapper;
import fit.bitjv.bookreview.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public List<UserResponseDto> getAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }
}
