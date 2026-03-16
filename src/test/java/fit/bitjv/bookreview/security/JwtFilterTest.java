package fit.bitjv.bookreview.security;

import fit.bitjv.bookreview.model.entity.Role;
import fit.bitjv.bookreview.model.entity.User;
import fit.bitjv.bookreview.service.AuthService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock JwtProvider jwtProvider;
    @Mock AuthService authService;
    @Mock JwtBlacklistService jwtBlacklistService;
    @Mock FilterChain filterChain;

    @InjectMocks JwtFilter jwtFilter;

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // valid token

    @Test
    void validToken_setsAuthenticationInSecurityContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = new User("alice", "alice@test.com", "pw");
        user.setRole(Role.USER);
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        when(jwtProvider.getTokenFromRequest(request)).thenReturn("valid-token");
        when(jwtProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtProvider.getUsernameFromToken("valid-token")).thenReturn("alice");
        when(authService.loadUserByUsername("alice")).thenReturn(userDetails);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
        verify(filterChain).doFilter(request, response);
    }

    // blacklisted token

    @Test
    void blacklistedToken_returns401AndStopsChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.getTokenFromRequest(request)).thenReturn("blacklisted-token");
        when(jwtProvider.validateToken("blacklisted-token")).thenReturn(true);
        when(jwtBlacklistService.isBlacklisted("blacklisted-token")).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    // missing token

    @Test
    void noToken_continuesChainWithoutSettingAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.getTokenFromRequest(request)).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // invalid token

    @Test
    void invalidToken_continuesChainWithoutSettingAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.getTokenFromRequest(request)).thenReturn("bad-token");
        when(jwtProvider.validateToken("bad-token")).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}