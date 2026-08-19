package com.aimes.security;

import com.aimes.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginProtectionServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LoginProtectionService loginProtectionService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void checkAllowed_shouldRejectLockedIp() {
        when(valueOperations.get("login:lock:ip:127.0.0.1")).thenReturn("1");

        assertThrows(BusinessException.class,
                () -> loginProtectionService.checkAllowed("127.0.0.1", "planner"));
    }

    @Test
    void checkAllowed_shouldRejectLockedUser() {
        when(valueOperations.get("login:lock:ip:127.0.0.1")).thenReturn(null);
        when(valueOperations.get("login:lock:user:planner")).thenReturn("1");

        assertThrows(BusinessException.class,
                () -> loginProtectionService.checkAllowed("127.0.0.1", "planner"));
    }

    @Test
    void captchaRequired_shouldBeTrueAfterThreeFailures() {
        when(valueOperations.get("login:fail:ip:127.0.0.1")).thenReturn("3");

        assertTrue(loginProtectionService.captchaRequired("127.0.0.1"));
    }

    @Test
    void captchaRequired_shouldBeFalseWhenBelowThreshold() {
        when(valueOperations.get("login:fail:ip:127.0.0.1")).thenReturn("2");

        assertFalse(loginProtectionService.captchaRequired("127.0.0.1"));
    }

    @Test
    void recordFailure_shouldLockUserAfterFiveAttempts() {
        when(valueOperations.increment("login:fail:ip:127.0.0.1")).thenReturn(1L);
        when(valueOperations.increment("login:fail:user:planner")).thenReturn(5L);

        loginProtectionService.recordFailure("127.0.0.1", "planner");

        verify(valueOperations).set(eq("login:lock:user:planner"), eq("1"), any(Duration.class));
    }

    @Test
    void clearOnSuccess_shouldDeleteFailureAndLockKeys() {
        loginProtectionService.clearOnSuccess("127.0.0.1", "planner");

        verify(stringRedisTemplate).delete("login:fail:ip:127.0.0.1");
        verify(stringRedisTemplate).delete("login:lock:ip:127.0.0.1");
        verify(stringRedisTemplate).delete("login:fail:user:planner");
        verify(stringRedisTemplate).delete("login:lock:user:planner");
    }

    @Test
    void checkAllowed_shouldPassWhenNotLocked() {
        when(valueOperations.get(anyString())).thenReturn(null);

        assertDoesNotThrow(() -> loginProtectionService.checkAllowed("127.0.0.1", "planner"));
    }
}
