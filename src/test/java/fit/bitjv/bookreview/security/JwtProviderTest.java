package fit.bitjv.bookreview.security;

import fit.bitjv.bookreview.model.entity.Role;
import fit.bitjv.bookreview.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtProviderTest {

    JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        // secret must be at least 32 bytes for HS256
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", "super-secret-key-that-is-long-enough-for-hs256");
        ReflectionTestUtils.setField(jwtProvider, "lifetime", 3_600_000L);
    }

    // token generation and validation

    @Test
    void generatedToken_isValid() {
        String token = jwtProvider.generateTokenFromUser(buildUser("alice"));

        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    void getUsernameFromToken_returnsCorrectUsername() {
        String token = jwtProvider.generateTokenFromUser(buildUser("alice"));

        assertThat(jwtProvider.getUsernameFromToken(token)).isEqualTo("alice");
    }

    @Test
    void validateToken_returnsFalseForTamperedToken() {
        String token = jwtProvider.generateTokenFromUser(buildUser("alice"));
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThat(jwtProvider.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_returnsFalseForGarbage() {
        assertThat(jwtProvider.validateToken("not.a.jwt")).isFalse();
    }

    // expiration

    @Test
    void expirationDate_isInFuture() {
        String token = jwtProvider.generateTokenFromUser(buildUser("alice"));

        assertThat(jwtProvider.getExpirationDateFromToken(token).getTime())
                .isGreaterThan(System.currentTimeMillis());
    }

    // token extraction from request

    @Test
    void getTokenFromRequest_extractsBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer my-token");

        assertThat(jwtProvider.getTokenFromRequest(request)).isEqualTo("my-token");
    }

    @Test
    void getTokenFromRequest_returnsNullWhenNoHeader() {
        assertThat(jwtProvider.getTokenFromRequest(new MockHttpServletRequest())).isNull();
    }

    @Test
    void getTokenFromRequest_returnsNullForNonBearerScheme() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        assertThat(jwtProvider.getTokenFromRequest(request)).isNull();
    }

    // helpers

    private User buildUser(String username) {
        User user = new User(username, username + "@test.com", "pw");
        user.setRole(Role.USER);
        return user;
    }
}