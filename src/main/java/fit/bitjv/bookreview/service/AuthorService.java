package fit.bitjv.bookreview.service;

import fit.bitjv.bookreview.model.mapper.AuthorMapper;
import fit.bitjv.bookreview.model.dto.response.AuthorResponseDto;
import fit.bitjv.bookreview.repository.AuthorRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final AuthorMapper mapper;

    public AuthorService(AuthorRepository authorRepository, AuthorMapper mapper) {
        this.authorRepository = authorRepository;
        this.mapper = mapper;
    }

    @Cacheable(value = "authors", key = "#id")
    public Optional<AuthorResponseDto> getById(Long id) {
        return authorRepository.findById(id).map(mapper::toDto);
    }

    public List<AuthorResponseDto> getAll() {
        return authorRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @CacheEvict(value = "authors", key = "#id")
    public void deleteById(Long id) {
        authorRepository.deleteById(id);
    }
}
