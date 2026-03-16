package fit.bitjv.bookreview.service;

import fit.bitjv.bookreview.exception.ResourceAlreadyExistsException;
import fit.bitjv.bookreview.model.entity.User;
import fit.bitjv.bookreview.repository.UserRepository;
import fit.bitjv.bookreview.security.UserDetailsImpl;
import fit.bitjv.bookreview.model.dto.EmailMessageDto;
import fit.bitjv.bookreview.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public User createUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new ResourceAlreadyExistsException("User with username " + username + " already exists");
        }

        User newUser = new User(username, email, passwordEncoder.encode(password));
        User savedUser = userRepository.save(newUser);
        log.info("Successfully registered new user: {}", username);

        EmailMessageDto welcomeEmail = new EmailMessageDto(
                email,
                "Welcome!",
                "Hi, " + username + "! Your account has been successfully created."
        );
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EMAIL_EXCHANGE,
                RabbitMqConfig.EMAIL_ROUTING_KEY,
                welcomeEmail);
        log.info("Welcome email sent to queue for user: {}", username);

        return savedUser;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Not found"));
        return new UserDetailsImpl(user);
    }
}