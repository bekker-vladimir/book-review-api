package fit.bitjv.bookreview.controller;

import fit.bitjv.bookreview.model.dto.response.AuthorResponseDto;
import fit.bitjv.bookreview.service.AuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/authors")
@Tag(name = "Author Controller", description = "CRUD operations for authors")
public class AuthorController {
    private final AuthorService authorService;

    @GetMapping
    @Operation(summary = "Get all authors")
    public ResponseEntity<List<AuthorResponseDto>> getAll() {
        return ResponseEntity.ok(authorService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get author by id")
    public ResponseEntity<AuthorResponseDto> getById(@PathVariable Long id) {
        return authorService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete author by id")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            authorService.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
