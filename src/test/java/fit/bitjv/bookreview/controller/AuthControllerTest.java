package fit.bitjv.bookreview.controller;

import fit.bitjv.bookreview.model.entity.Role;
import fit.bitjv.bookreview.model.entity.User;
import fit.bitjv.bookreview.security.JwtBlacklistService;
import fit.bitjv.bookreview.security.JwtFilter;
import fit.bitjv.bookreview.security.JwtProvider;
import fit.bitjv.bookreview.security.SecurityConfig;
import fit.bitjv.bookreview.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AuthService authService;

    @MockBean
    AuthenticationManager authenticationManager;

    @MockBean
    JwtProvider jwtProvider;

    @MockBean
    JwtBlacklistService jwtBlacklistService;

    // register

    @Test
    void register_returns200WithToken() throws Exception {
        User user = new User("alice", "alice@test.com", "pw");
        user.setRole(Role.USER);
        when(authService.createUser("alice", "alice@test.com", "password123")).thenReturn(user);
        when(jwtProvider.generateTokenFromUser(user)).thenReturn("jwt-token");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice@test.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("jwt-token"));
    }

    @Test
    void register_returns400WhenBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // login

    @Test
    void login_returns200WithToken() throws Exception {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("alice", null, null);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtProvider.generateToken(auth)).thenReturn("jwt-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("jwt-token"));
    }

    @Test
    void login_returns401WhenCredentialsAreInvalid() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"alice","password":"wrongpassword"}
                            """))
                .andExpect(status().isUnauthorized());
    }

    // logout

    @Test
    @WithMockUser
    void logout_blacklistsTokenAndReturns200() throws Exception {
        when(jwtProvider.getTokenFromRequest(any())).thenReturn("some-token");
        when(jwtProvider.getExpirationDateFromToken("some-token"))
                .thenReturn(new Date(System.currentTimeMillis() + 60_000L));

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer some-token"))
                .andExpect(status().isOk());

        verify(jwtBlacklistService).addToBlacklist(eq("some-token"), anyLong());
    }
}