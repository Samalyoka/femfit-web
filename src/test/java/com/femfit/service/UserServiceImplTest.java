package com.femfit.service;

import com.femfit.dao.UserDao;
import com.femfit.dto.RegisterDto;
import com.femfit.exception.EmailAlreadyTakenException;
import com.femfit.exception.InvalidPasswordException;
import com.femfit.model.Role;
import com.femfit.model.User;
import com.femfit.service.impl.UserServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserServiceImpl}.
 * Uses Mockito to isolate the service from the DAO and PasswordEncoder.
 * Covers positive, negative, and edge-case scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl tests")
class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterDto validDto;

    @BeforeEach
    void setUp() {
        validDto = new RegisterDto();
        validDto.setFirstName("Anna");
        validDto.setLastName("Kim");
        validDto.setEmail("anna@test.kz");
        validDto.setPassword("securePass123");
        validDto.setPhone("+77001234567");
        validDto.setBirthDate(LocalDate.of(1998, 5, 20));
    }

    // ── register ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: success — new user is saved with hashed password")
    void register_success() {
        when(userDao.existsByEmail(validDto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validDto.getPassword())).thenReturn("$2a$12$hashed");
        User savedUser = User.builder().id(1L).email(validDto.getEmail()).role(Role.CLIENT).build();
        when(userDao.save(any(User.class))).thenReturn(savedUser);

        User result = userService.register(validDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRole()).isEqualTo(Role.CLIENT);
        verify(userDao).save(argThat(u ->
                u.getPasswordHash().equals("$2a$12$hashed") &&
                        u.getEmail().equals(validDto.getEmail())
        ));
    }

    @Test
    @DisplayName("register: throws EmailAlreadyTakenException when email exists")
    void register_emailTaken_throwsException() {
        when(userDao.existsByEmail(validDto.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(validDto))
                .isInstanceOf(EmailAlreadyTakenException.class)
                .hasMessageContaining(validDto.getEmail());

        verify(userDao, never()).save(any());
    }

    @Test
    @DisplayName("register: plain-text password is never stored")
    void register_passwordIsHashed() {
        when(userDao.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashed");
        when(userDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.register(validDto);

        verify(userDao).save(argThat(u ->
                !u.getPasswordHash().equals(validDto.getPassword())
        ));
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById: returns user when found")
    void findById_found() {
        User user = User.builder().id(1L).email("anna@test.kz").build();
        when(userDao.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById: returns empty when not found")
    void findById_notFound() {
        when(userDao.findById(999L)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(999L);

        assertThat(result).isEmpty();
    }

    // ── changePassword ────────────────────────────────────────────────────

    @Test
    @DisplayName("changePassword: success when old password matches")
    void changePassword_success() {
        User user = User.builder().id(1L).passwordHash("$2a$hash").build();
        when(userDao.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "$2a$hash")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("$2a$newHash");

        userService.changePassword(1L, "oldPass", "newPass");

        verify(userDao).updatePassword(1L, "$2a$newHash");
    }

    @Test
    @DisplayName("changePassword: throws InvalidPasswordException when old password is wrong")
    void changePassword_wrongOldPassword() {
        User user = User.builder().id(1L).passwordHash("$2a$hash").build();
        when(userDao.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, "wrongPass", "newPass"))
                .isInstanceOf(InvalidPasswordException.class);

        verify(userDao, never()).updatePassword(anyLong(), anyString());
    }

    // ── setDiscount ───────────────────────────────────────────────────────

    @Test
    @DisplayName("setDiscount: success for valid discount 0-100")
    void setDiscount_valid() {
        userService.setDiscount(1L, 20);
        verify(userDao).setDiscount(1L, 20);
    }

    @Test
    @DisplayName("setDiscount: throws IllegalArgumentException for discount > 100")
    void setDiscount_invalidValue() {
        assertThatThrownBy(() -> userService.setDiscount(1L, 150))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userDao, never()).setDiscount(anyLong(), anyInt());
    }

    @Test
    @DisplayName("setDiscount: throws IllegalArgumentException for negative discount")
    void setDiscount_negative() {
        assertThatThrownBy(() -> userService.setDiscount(1L, -5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}