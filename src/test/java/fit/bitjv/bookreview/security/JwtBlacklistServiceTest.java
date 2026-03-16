package fit.bitjv.bookreview.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks JwtBlacklistService jwtBlacklistService;

    // addToBlacklist

    @Test
    void addToBlacklist_storesTokenWithPositiveTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        long futureExpiry = System.currentTimeMillis() + 60_000L;

        jwtBlacklistService.addToBlacklist("some-token", futureExpiry);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(eq("jwt:blacklist:some-token"), eq("true"), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue().toMillis()).isPositive();
    }

    @Test
    void addToBlacklist_doesNothingWhenTokenAlreadyExpired() {
        long pastExpiry = System.currentTimeMillis() - 1_000L;

        jwtBlacklistService.addToBlacklist("expired-token", pastExpiry);

        verifyNoInteractions(redisTemplate);
    }

    // isBlacklisted

    @Test
    void isBlacklisted_returnsTrueWhenKeyExists() {
        when(redisTemplate.hasKey("jwt:blacklist:my-token")).thenReturn(true);

        assertThat(jwtBlacklistService.isBlacklisted("my-token")).isTrue();
    }

    @Test
    void isBlacklisted_returnsFalseWhenKeyAbsent() {
        when(redisTemplate.hasKey("jwt:blacklist:clean-token")).thenReturn(false);

        assertThat(jwtBlacklistService.isBlacklisted("clean-token")).isFalse();
    }
}