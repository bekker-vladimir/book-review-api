package fit.bitjv.bookreview.service;

import fit.bitjv.bookreview.config.RabbitMqConfig;
import fit.bitjv.bookreview.exception.ResourceAlreadyExistsException;
import fit.bitjv.bookreview.model.dto.EmailMessageDto;
import fit.bitjv.bookreview.model.entity.User;
import fit.bitjv.bookreview.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RabbitTemplate rabbitTemplate;

    @InjectMocks AuthService authService;

    // createUser

    @Test
    void createUser_success() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        User saved = new User("alice", "alice@example.com", "hashed");
        when(userRepository.save(any())).thenReturn(saved);

        User result = authService.createUser("alice", "alice@example.com", "secret");

        assertThat(result.getUsername()).isEqualTo("alice");
    }

    @Test
    void createUser_sendsWelcomeEmailToQueue() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(new User("alice", "alice@example.com", "hashed"));

        authService.createUser("alice", "alice@example.com", "secret");

        ArgumentCaptor<EmailMessageDto> captor = ArgumentCaptor.forClass(EmailMessageDto.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.EMAIL_EXCHANGE),
                eq(RabbitMqConfig.EMAIL_ROUTING_KEY),
                captor.capture()
        );
        assertThat(captor.getValue().to()).isEqualTo("alice@example.com");
    }

    @Test
    void createUser_throwsWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.createUser("alice", "alice@example.com", "secret"))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("alice");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(rabbitTemplate);
    }

    // loadUserByUsername

    @Test
    void loadUserByUsername_returnsUserDetails() {
        User user = new User("alice", "alice@example.com", "hashed");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        var details = authService.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
    }

    @Test
    void loadUserByUsername_throwsWhenNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}