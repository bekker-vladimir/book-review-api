package fit.bitjv.bookreview.service;

import fit.bitjv.bookreview.model.dto.response.AuthorResponseDto;
import fit.bitjv.bookreview.model.entity.Author;
import fit.bitjv.bookreview.model.mapper.AuthorMapper;
import fit.bitjv.bookreview.repository.AuthorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock AuthorRepository authorRepository;
    @Mock AuthorMapper mapper;

    @InjectMocks AuthorService authorService;

    // getById

    @Test
    void getById_returnsMappedDto() {
        Author author = new Author();
        author.setFullName("Robert Martin");
        AuthorResponseDto dto = new AuthorResponseDto();

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(mapper.toDto(author)).thenReturn(dto);

        assertThat(authorService.getById(1L)).contains(dto);
    }

    @Test
    void getById_returnsEmptyWhenNotFound() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(authorService.getById(99L)).isEmpty();
    }

    // getAll

    @Test
    void getAll_returnsMappedList() {
        Author a1 = new Author();
        Author a2 = new Author();
        when(authorRepository.findAll()).thenReturn(List.of(a1, a2));
        when(mapper.toDto(a1)).thenReturn(new AuthorResponseDto());
        when(mapper.toDto(a2)).thenReturn(new AuthorResponseDto());

        assertThat(authorService.getAll()).hasSize(2);
    }

    @Test
    void getAll_returnsEmptyListWhenNoAuthors() {
        when(authorRepository.findAll()).thenReturn(List.of());

        assertThat(authorService.getAll()).isEmpty();
    }

    // deleteById

    @Test
    void deleteById_delegatesToRepository() {
        authorService.deleteById(1L);

        verify(authorRepository).deleteById(1L);
    }
}