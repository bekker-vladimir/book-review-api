package fit.bitjv.bookreview.exception;

import fit.bitjv.bookreview.model.dto.response.ErrorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // AuthenticationException

    @Test
    void handleSpringSecurityAuthError_returns401WithGenericMessage() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleSpringSecurityAuthError(new BadCredentialsException("Bad credentials"));
        ErrorResponseDto body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(401);
        assertThat(body.getMessage()).isEqualTo("Invalid username or password");
        assertThat(body.getTimestamp()).isNotNull();
    }

    // UnauthorizedAccessException

    @Test
    void handleUnauthorizedAccess_returns401WithExceptionMessage() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleUnauthorizedAccessError(new UnauthorizedAccessException("Session expired"));
        ErrorResponseDto body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(401);
        assertThat(body.getMessage()).isEqualTo("Session expired");
    }

    // ResourceNotFoundException

    @Test
    void handleResourceNotFound_returns404ContainingResourceInfo() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleResourceNotFoundError(new ResourceNotFoundException("Book", "id", 42L));
        ErrorResponseDto body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getMessage()).contains("Book").contains("42");
    }

    // ResourceAlreadyExistsException

    @Test
    void handleResourceAlreadyExists_returns409WithExceptionMessage() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleResourceAlreadyExistsException(new ResourceAlreadyExistsException("User already exists"));
        ErrorResponseDto body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(409);
        assertThat(body.getMessage()).isEqualTo("User already exists");
    }

    // MethodArgumentNotValidException

    @Test
    void handleValidation_returns400WithAllFieldErrorsConcatenated() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "username", "must not be blank"),
                new FieldError("obj", "email", "must be a valid email")
        ));

        ResponseEntity<ErrorResponseDto> response = handler.handleValidationException(ex);
        ErrorResponseDto body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getMessage())
                .contains("must not be blank")
                .contains("must be a valid email");
    }

    @Test
    void handleValidation_returns400WithSingleFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(
                List.of(new FieldError("obj", "password", "size must be between 8 and 64"))
        );

        ResponseEntity<ErrorResponseDto> response = handler.handleValidationException(ex);
        ErrorResponseDto body = response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("size must be between 8 and 64");
    }

    // IllegalArgumentException

    @Test
    void handleIllegalArgument_returns400WithExceptionMessage() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("Invalid status value: UNKNOWN"));
        ErrorResponseDto body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getMessage()).isEqualTo("Invalid status value: UNKNOWN");
    }

    // catch-all

    @Test
    void handleUnexpectedException_returns500WithGenericMessage() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleAllUnhandledExceptions(new RuntimeException("Something went very wrong"));
        ErrorResponseDto body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getMessage()).isEqualTo("Internal Server Error");
    }

    // exception classes

    @Test
    void resourceNotFoundException_messageContainsAllParts() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Review", "id", 7L);

        assertThat(ex.getMessage()).contains("Review").contains("id").contains("7");
    }

    @Test
    void resourceAlreadyExistsException_preservesMessage() {
        assertThat(new ResourceAlreadyExistsException("Duplicate entry").getMessage())
                .isEqualTo("Duplicate entry");
    }

    @Test
    void unauthorizedAccessException_preservesMessage() {
        assertThat(new UnauthorizedAccessException("Access denied").getMessage())
                .isEqualTo("Access denied");
    }
}