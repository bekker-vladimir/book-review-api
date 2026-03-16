package fit.bitjv.bookreview.controller;

import fit.bitjv.bookreview.model.dto.request.LoginRequestDto;
import fit.bitjv.bookreview.model.dto.request.RegisterRequestDto;
import fit.bitjv.bookreview.model.entity.User;
import fit.bitjv.bookreview.security.JwtBlacklistService;
import fit.bitjv.bookreview.security.JwtProvider;
import fit.bitjv.bookreview.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final JwtBlacklistService jwtBlacklistService;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager, JwtProvider jwtProvider, JwtBlacklistService jwtBlacklistService) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDto registerRequest) {
        User newUser = authService.createUser(registerRequest.getUsername(), registerRequest.getEmail(), registerRequest.getPassword());

        String token = jwtProvider.generateTokenFromUser(newUser);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        String token = jwtProvider.generateToken(authentication);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = jwtProvider.getTokenFromRequest(request);

        long expiration = jwtProvider.getExpirationDateFromToken(token).getTime();
        jwtBlacklistService.addToBlacklist(token, expiration);

        return ResponseEntity.ok("Successfully logged out");
    }
}