package fit.bitjv.bookreview.service;

import fit.bitjv.bookreview.exception.ResourceNotFoundException;
import fit.bitjv.bookreview.model.dto.request.BookRequestDto;
import fit.bitjv.bookreview.model.dto.response.BookResponseDto;
import fit.bitjv.bookreview.model.mapper.BookMapper;
import fit.bitjv.bookreview.model.entity.Author;
import fit.bitjv.bookreview.model.entity.Book;
import fit.bitjv.bookreview.model.entity.BookStatus;
import fit.bitjv.bookreview.repository.AuthorRepository;
import fit.bitjv.bookreview.repository.BookRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookMapper bookMapper;
    private final Path coverStorageLocation;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository, BookMapper bookMapper,
                       @Value("${book.cover.upload-dir}") String uploadDir) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.bookMapper = bookMapper;
        this.coverStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.coverStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Transactional
    public BookResponseDto createBook(BookRequestDto bookRequestDto) {
        Set<String> requestedAuthorNames = bookRequestDto.getAuthorNames();
        List<Author> existingAuthors = authorRepository.findAllByFullNameIn(requestedAuthorNames);
        Set<String> existingNames = existingAuthors.stream()
                .map(Author::getFullName)
                .collect(Collectors.toSet());

        List<Author> newAuthorsToSave = requestedAuthorNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> {
                    Author author = new Author();
                    author.setFullName(name);
                    return author;
                })
                .toList();
        if (!newAuthorsToSave.isEmpty()) {
            newAuthorsToSave = authorRepository.saveAll(newAuthorsToSave);
        }

        Set<Author> finalAuthors = new HashSet<>(existingAuthors);
        finalAuthors.addAll(newAuthorsToSave);

        Book book = new Book();
        book.setTitle(bookRequestDto.getTitle());
        book.setGenre(bookRequestDto.getGenre());
        book.setDescription(bookRequestDto.getDescription());
        book.setPublicationDate(bookRequestDto.getPublicationDate());
        book.setAuthors(finalAuthors);

        Book saved = bookRepository.save(book);
        return bookMapper.toDto(saved);
    }

    @Cacheable(value = "books", key = "#id")
    public BookResponseDto getById(Long id) {
        return bookRepository.findByIdWithAuthors(id)
                .map(bookMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));
    }

    /**
     * Two-step pagination - fixes the HHH90003004 (in-memory pagination) issue:
     * Step 1: Fetch a page of IDs without JOINs (allows true SQL LIMIT/OFFSET).
     * Step 2: Fetch full entities by those IDs in a single query using JOIN FETCH.
     */
    @Transactional
    public Page<BookResponseDto> getAllPaged(Pageable pageable) {
        Page<Long> idPage = bookRepository.findIdsByStatus(BookStatus.APPROVED, pageable);
        return buildPageFromIds(idPage, pageable);
    }

    public List<BookResponseDto> getTopRated(int count) {
        List<Long> ids = bookRepository.findTopRatedIds(count);
        if (ids.isEmpty()) {
            return List.of();
        }
        return fetchAndOrderByIds(ids);
    }

    public List<BookResponseDto> getRecentlyAdded(int count) {
        List<Long> ids = bookRepository.findRecentlyAddedIds(BookStatus.APPROVED, count);
        if (ids.isEmpty()){
            return List.of();
        }
        return fetchAndOrderByIds(ids);
    }

    @Transactional
    public Page<BookResponseDto> searchBooksPaged(String query, Pageable pageable) {
        Page<Long> idPage = bookRepository.findIdsByTitleOrAuthorAndStatus(query, BookStatus.APPROVED, pageable);
        return buildPageFromIds(idPage, pageable);
    }

    /**
     * Helper method: Constructs a Page of DTOs from a Page of IDs.
     * Handles empty results and delegates entity fetching and ordering.
     */
    private Page<BookResponseDto> buildPageFromIds(Page<Long> idPage, Pageable pageable) {
        List<Long> ids = idPage.getContent();
        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }
        List<BookResponseDto> dtos = fetchAndOrderByIds(ids);
        return new PageImpl<>(dtos, pageable, idPage.getTotalElements());
    }

    /**
     * Helper method: Fetches books by IDs with authors and restores
     * the original sorting order (matching the paginated ID query).
     */
    private List<BookResponseDto> fetchAndOrderByIds(List<Long> orderedIds) {
        List<Book> books = bookRepository.findAllWithAuthorsByIds(orderedIds);
        Map<Long, Book> bookMap = books.stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));
        return orderedIds.stream()
                .map(bookMap::get)
                .filter(Objects::nonNull)
                .map(bookMapper::toDto)
                .toList();
    }

    public List<BookResponseDto> getAllByStatus(BookStatus status) {
        return bookRepository.findAllWithAuthorsByStatus(status)
                .stream()
                .map(bookMapper::toDto)
                .toList();
    }

    @CacheEvict(value = "books", key = "#id")
    public void deleteById(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book", "id", id);
        }
        bookRepository.deleteById(id);
    }

    @Transactional
    @CacheEvict(value = "books", key = "#id")
    public void changeStatus(Long id, BookStatus status) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));

        if(status == BookStatus.APPROVED){
            book.setApprovedAt(LocalDateTime.now());
        }

        book.setStatus(status);
        bookRepository.save(book);
    }

    @Transactional
    @CacheEvict(value = "books", key = "#id")
    public String uploadCover(Long id, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file.");
        }
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));

        try {
            String originalFilename = file.getOriginalFilename();

            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String filename = UUID.randomUUID() + fileExtension;

            Path targetLocation = this.coverStorageLocation.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String coverPath = targetLocation.toString();
            book.setCoverPath(coverPath);
            bookRepository.save(book);

            return coverPath;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    public Resource getCoverFile(String filename) {
        try {
            Path filePath = this.coverStorageLocation.resolve(filename).normalize();

            if (!filePath.getParent().equals(this.coverStorageLocation)) {
                throw new SecurityException("Cannot access file outside of current directory.");
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File", "filename", filename);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File", "filename", filename);
        }
    }

    public ResponseEntity<Resource> getCoverResponse(String filename) {
        Resource file = getCoverFile(filename);
        String contentType;

        try {
            contentType = Files.probeContentType(file.getFile().toPath());
            if (contentType == null) contentType = "application/octet-stream";
        } catch (IOException ex) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    public BookStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        try {
            return BookStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + statusStr);
        }
    }
}