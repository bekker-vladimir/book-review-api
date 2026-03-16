package fit.bitjv.bookreview.controller;

import fit.bitjv.bookreview.exception.ResourceNotFoundException;
import fit.bitjv.bookreview.model.dto.response.BookResponseDto;
import fit.bitjv.bookreview.model.entity.BookStatus;
import fit.bitjv.bookreview.security.JwtBlacklistService;
import fit.bitjv.bookreview.security.JwtFilter;
import fit.bitjv.bookreview.security.JwtProvider;
import fit.bitjv.bookreview.security.SecurityConfig;
import fit.bitjv.bookreview.service.AuthService;
import fit.bitjv.bookreview.service.BookService;
import fit.bitjv.bookreview.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class BookControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    BookService bookService;

    @MockBean
    ReviewService reviewService;

    @MockBean
    JwtProvider jwtProvider;

    @MockBean
    JwtBlacklistService jwtBlacklistService;

    @MockBean
    AuthService authService;

    // GET /books

    @Test
    @WithMockUser
    void getAll_returnsPagedBooks() throws Exception {
        when(bookService.getAllPaged(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new BookResponseDto())));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // GET /books/{id}

    @Test
    @WithMockUser
    void getById_returns200WhenFound() throws Exception {
        when(bookService.getById(1L)).thenReturn(new BookResponseDto());

        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getById_returns404WhenNotFound() throws Exception {
        when(bookService.getById(99L)).thenThrow(new ResourceNotFoundException("Book", "id", 99L));

        mockMvc.perform(get("/books/99"))
                .andExpect(status().isNotFound());
    }

    // POST /books

    @Test
    @WithMockUser
    void createBook_returns201WithBody() throws Exception {
        when(bookService.createBook(any())).thenReturn(new BookResponseDto());

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Clean Code",
                                  "genre": "PROGRAMMING",
                                  "authorNames": ["Robert Martin"],
                                  "publicationDate": "1999-01-01"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    // PATCH /books/{id}/status

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeStatus_returns200ForAdmin() throws Exception {
        when(bookService.parseStatus("APPROVED")).thenReturn(BookStatus.APPROVED);
        doNothing().when(bookService).changeStatus(eq(1L), any());

        mockMvc.perform(patch("/books/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"APPROVED"}
                                """))
                .andExpect(status().isOk());
    }

    // GET /books/pending

    @Test
    @WithMockUser(roles = "USER")
    void getPending_returns403ForRegularUser() throws Exception {
        mockMvc.perform(get("/books/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void getPending_returns200ForModerator() throws Exception {
        when(bookService.getAllByStatus(any())).thenReturn(List.of());

        mockMvc.perform(get("/books/pending"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getPending_returns200ForAdmin() throws Exception {
        when(bookService.getAllByStatus(any())).thenReturn(List.of());

        mockMvc.perform(get("/books/pending"))
                .andExpect(status().isOk());
    }
}