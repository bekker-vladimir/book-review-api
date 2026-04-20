package fit.bitjv.bookreview.service;

import fit.bitjv.bookreview.exception.ResourceNotFoundException;
import fit.bitjv.bookreview.model.dto.request.BookRequestDto;
import fit.bitjv.bookreview.model.dto.response.BookResponseDto;
import fit.bitjv.bookreview.model.entity.Author;
import fit.bitjv.bookreview.model.entity.Book;
import fit.bitjv.bookreview.model.entity.BookStatus;
import fit.bitjv.bookreview.model.mapper.BookMapper;
import fit.bitjv.bookreview.repository.AuthorRepository;
import fit.bitjv.bookreview.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock BookRepository bookRepository;
    @Mock AuthorRepository authorRepository;
    @Mock BookMapper bookMapper;

    // BookService requires an upload dir path - inject via constructor in setUp
    BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, authorRepository, bookMapper,
                System.getProperty("java.io.tmpdir"));
    }

    // getById

    @Test
    void getById_returnsDto() {
        Book book = new Book();
        BookResponseDto dto = new BookResponseDto();
        when(bookRepository.findByIdWithAuthors(1L)).thenReturn(Optional.of(book));
        when(bookMapper.toDto(book)).thenReturn(dto);

        assertThat(bookService.getById(1L)).isSameAs(dto);
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(bookRepository.findByIdWithAuthors(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // createBook

    @Test
    void createBook_savesNewAuthorsForUnknownNames() {
        BookRequestDto dto = new BookRequestDto();
        dto.setTitle("Clean Code");
        dto.setAuthorNames(Set.of("Robert Martin", "Unknown Author"));

        Author existing = new Author();
        existing.setFullName("Robert Martin");
        when(authorRepository.findAllByFullNameIn(anySet())).thenReturn(List.of(existing));
        when(authorRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(bookRepository.save(any())).thenReturn(new Book());
        when(bookMapper.toDto(any())).thenReturn(new BookResponseDto());

        bookService.createBook(dto);

        // only the unknown author should be passed to saveAll
        verify(authorRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1));
    }

    @Test
    void createBook_skipsAuthorSaveWhenAllExist() {
        BookRequestDto dto = new BookRequestDto();
        dto.setTitle("Clean Code");
        dto.setAuthorNames(Set.of("Robert Martin"));

        Author existing = new Author();
        existing.setFullName("Robert Martin");
        when(authorRepository.findAllByFullNameIn(anySet())).thenReturn(List.of(existing));
        when(bookRepository.save(any())).thenReturn(new Book());
        when(bookMapper.toDto(any())).thenReturn(new BookResponseDto());

        bookService.createBook(dto);

        verify(authorRepository, never()).saveAll(anyList());
    }

    // deleteById

    @Test
    void deleteById_throwsWhenNotFound() {
        when(bookRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.deleteById(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteById_deletesWhenExists() {
        when(bookRepository.existsById(5L)).thenReturn(true);

        bookService.deleteById(5L);

        verify(bookRepository).deleteById(5L);
    }

    // changeStatus

    @Test
    void changeStatus_updatesAndSavesBook() {
        Book book = new Book();
        book.setStatus(BookStatus.PENDING);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        bookService.changeStatus(1L, BookStatus.APPROVED);

        assertThat(book.getStatus()).isEqualTo(BookStatus.APPROVED);
        verify(bookRepository).save(book);
    }

    @Test
    void changeStatus_throwsWhenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.changeStatus(99L, BookStatus.APPROVED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // parseStatus

    @Test
    void parseStatus_returnsCorrectEnum() {
        assertThat(bookService.parseStatus("approved")).isEqualTo(BookStatus.APPROVED);
        assertThat(bookService.parseStatus("PENDING")).isEqualTo(BookStatus.PENDING);
    }

    @Test
    void parseStatus_throwsOnUnknownValue() {
        assertThatThrownBy(() -> bookService.parseStatus("INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID");
    }

    @Test
    void parseStatus_throwsOnBlankValue() {
        assertThatThrownBy(() -> bookService.parseStatus(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}