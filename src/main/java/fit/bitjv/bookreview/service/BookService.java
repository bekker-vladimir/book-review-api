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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
        return bookRepository.findById(id)
                .map(bookMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));
    }

    public List<BookResponseDto> getAll() {
        return bookRepository.findAllWithAuthorsByStatus(BookStatus.APPROVED)
                .stream()
                .map(bookMapper::toDto)
                .toList();
    }

    public Page<BookResponseDto> getAllPaged(Pageable pageable) {
        return bookRepository.findAllWithAuthorsByStatus(BookStatus.APPROVED, pageable)
                .map(bookMapper::toDto);
    }

    public Page<BookResponseDto> searchBooksPaged(String query, Pageable pageable) {
        return bookRepository.searchByTitleOrAuthorAndStatus(query, BookStatus.APPROVED, pageable)
                .map(bookMapper::toDto);
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

    public List<BookResponseDto> searchBooks(String query) {
        return bookRepository.searchByTitleOrAuthorAndStatus(query, BookStatus.APPROVED)
                .stream()
                .map(bookMapper::toDto)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "books", key = "#id")
    public void changeStatus(Long id, BookStatus status) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));
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