package com.femfit.service;

import com.femfit.dao.MemberDao;
import com.femfit.dao.OrderDao;
import com.femfit.dto.PageDto;
import com.femfit.dto.RegisterDto;
import com.femfit.exception.EmailAlreadyTakenException;
import com.femfit.exception.InvalidPasswordException;
import com.femfit.model.AccountType;
import com.femfit.model.Role;
import com.femfit.model.Member;
import com.femfit.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MemberServiceImpl}.
 * Uses Mockito to isolate the service from the DAO and PasswordEncoder.
 * Covers positive, negative, and edge-case scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberServiceImpl tests")
class MemberServiceImplTest {

    @Mock
    private MemberDao memberDao;

    @Mock
    private OrderDao orderDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberServiceImpl userService;

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
        when(memberDao.existsByEmail(validDto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validDto.getPassword())).thenReturn("$2a$12$hashed");
        Member savedMember = Member.builder().id(1L).email(validDto.getEmail()).role(Role.CLIENT).build();
        when(memberDao.save(any(Member.class))).thenReturn(savedMember);

        Member result = userService.register(validDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRole()).isEqualTo(Role.CLIENT);
        verify(memberDao).save(argThat(u ->
                u.getPasswordHash().equals("$2a$12$hashed") &&
                        u.getEmail().equals(validDto.getEmail()) &&
                        u.getAccountType() == AccountType.REGULAR
        ));
    }

    @Test
    @DisplayName("register: throws EmailAlreadyTakenException when email exists")
    void register_emailTaken_throwsException() {
        when(memberDao.existsByEmail(validDto.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(validDto))
                .isInstanceOf(EmailAlreadyTakenException.class)
                .hasMessageContaining(validDto.getEmail());

        verify(memberDao, never()).save(any());
    }

    @Test
    @DisplayName("register: plain-text password is never stored")
    void register_passwordIsHashed() {
        when(memberDao.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashed");
        when(memberDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.register(validDto);

        verify(memberDao).save(argThat(u ->
                !u.getPasswordHash().equals(validDto.getPassword())
        ));
    }

    @Test
    @DisplayName("register: succeeds when optional fields (phone, birthDate) are null")
    void register_optionalFieldsNull() {
        validDto.setPhone(null);
        validDto.setBirthDate(null);

        when(memberDao.existsByEmail(validDto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validDto.getPassword())).thenReturn("$2a$12$hashed");
        when(memberDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Member result = userService.register(validDto);

        assertThat(result.getPhone()).isNull();
        assertThat(result.getBirthDate()).isNull();
        assertThat(result.getEmail()).isEqualTo(validDto.getEmail());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById: returns user when found")
    void findById_found() {
        Member member = Member.builder().id(1L).email("anna@test.kz").build();
        when(memberDao.findById(1L)).thenReturn(Optional.of(member));

        Optional<Member> result = userService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById: returns empty when not found")
    void findById_notFound() {
        when(memberDao.findById(999L)).thenReturn(Optional.empty());

        Optional<Member> result = userService.findById(999L);

        assertThat(result).isEmpty();
    }

    // ── findByRole ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByRole: returns empty page when no members have the given role")
    void findByRole_empty() {
        when(memberDao.findByRole(Role.TRAINER, 0, 10)).thenReturn(Collections.emptyList());
        when(memberDao.countByRole(Role.TRAINER)).thenReturn(0);

        PageDto<Member> result = userService.findByRole(Role.TRAINER, 1, 10);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalItems()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    // ── changePassword ────────────────────────────────────────────────────

    @Test
    @DisplayName("changePassword: success when old password matches")
    void changePassword_success() {
        Member member = Member.builder().id(1L).passwordHash("$2a$hash").build();
        when(memberDao.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("oldPass", "$2a$hash")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("$2a$newHash");

        userService.changePassword(1L, "oldPass", "newPass");

        verify(memberDao).updatePassword(1L, "$2a$newHash");
    }

    @Test
    @DisplayName("changePassword: throws InvalidPasswordException when old password is wrong")
    void changePassword_wrongOldPassword() {
        Member member = Member.builder().id(1L).passwordHash("$2a$hash").build();
        when(memberDao.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrongPass", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, "wrongPass", "newPass"))
                .isInstanceOf(InvalidPasswordException.class);

        verify(memberDao, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    @DisplayName("changePassword: throws RuntimeException when member not found")
    void changePassword_memberNotFound() {
        when(memberDao.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(404L, "old", "new"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("404");

        verify(memberDao, never()).updatePassword(anyLong(), anyString());
    }

    // ── setDiscount ───────────────────────────────────────────────────────

    @Test
    @DisplayName("setDiscount: success for valid discount 0-100")
    void setDiscount_valid() {
        userService.setDiscount(1L, 20);
        verify(memberDao).setDiscount(1L, 20);
    }

    @Test
    @DisplayName("setDiscount: boundary value 0 is accepted")
    void setDiscount_zeroBoundary() {
        userService.setDiscount(1L, 0);
        verify(memberDao).setDiscount(1L, 0);
    }

    @Test
    @DisplayName("setDiscount: boundary value 100 is accepted")
    void setDiscount_hundredBoundary() {
        userService.setDiscount(1L, 100);
        verify(memberDao).setDiscount(1L, 100);
    }

    @Test
    @DisplayName("setDiscount: throws IllegalArgumentException for discount > 100")
    void setDiscount_invalidValue() {
        assertThatThrownBy(() -> userService.setDiscount(1L, 150))
                .isInstanceOf(IllegalArgumentException.class);
        verify(memberDao, never()).setDiscount(anyLong(), anyInt());
    }

    @Test
    @DisplayName("setDiscount: throws IllegalArgumentException for discount of 101 (just over boundary)")
    void setDiscount_justOverBoundary() {
        assertThatThrownBy(() -> userService.setDiscount(1L, 101))
                .isInstanceOf(IllegalArgumentException.class);
        verify(memberDao, never()).setDiscount(anyLong(), anyInt());
    }

    @Test
    @DisplayName("setDiscount: throws IllegalArgumentException for negative discount")
    void setDiscount_negative() {
        assertThatThrownBy(() -> userService.setDiscount(1L, -5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("setDiscount: throws IllegalArgumentException for discount of -1 (just under boundary)")
    void setDiscount_justUnderBoundary() {
        assertThatThrownBy(() -> userService.setDiscount(1L, -1))
                .isInstanceOf(IllegalArgumentException.class);
        verify(memberDao, never()).setDiscount(anyLong(), anyInt());
    }

    // ── setAccountType ───────────────────────────────────────────────────

    @Test
    @DisplayName("setAccountType: persists the new type and recalculates discount")
    void setAccountType_persistsAndRecalculates() {
        Member member = Member.builder().id(1L).accountType(AccountType.CORPORATE).build();
        when(memberDao.findById(1L)).thenReturn(Optional.of(member));
        when(orderDao.countCompletedByUserId(1L)).thenReturn(0);

        userService.setAccountType(1L, AccountType.CORPORATE);

        verify(memberDao).setAccountType(1L, AccountType.CORPORATE);
        // recalculateDiscount runs after — CORPORATE with 0 cycles still gets the flat 10%
        verify(memberDao).setDiscount(1L, 10);
    }

    @Test
    @DisplayName("setAccountType: switching back to REGULAR recalculates using completed-cycle tiers")
    void setAccountType_switchToRegularRecalculates() {
        Member member = Member.builder().id(1L).accountType(AccountType.REGULAR).build();
        when(memberDao.findById(1L)).thenReturn(Optional.of(member));
        when(orderDao.countCompletedByUserId(1L)).thenReturn(7);

        userService.setAccountType(1L, AccountType.REGULAR);

        verify(memberDao).setAccountType(1L, AccountType.REGULAR);
        verify(memberDao).setDiscount(1L, 10);
    }

    // ── calculateAutoDiscount ────────────────────────────────────────────

    @Test
    @DisplayName("calculateAutoDiscount: CORPORATE always gets flat 10%, regardless of completed cycles")
    void calculateAutoDiscount_corporateFlatTen() {
        Member member = Member.builder().accountType(AccountType.CORPORATE).build();

        assertThat(userService.calculateAutoDiscount(member, 0)).isEqualTo(10);
        assertThat(userService.calculateAutoDiscount(member, 50)).isEqualTo(10);
    }

    @Test
    @DisplayName("calculateAutoDiscount: REGULAR with 0-2 completed cycles gets 0%")
    void calculateAutoDiscount_regularBelowFirstTier() {
        Member member = Member.builder().accountType(AccountType.REGULAR).build();

        assertThat(userService.calculateAutoDiscount(member, 0)).isZero();
        assertThat(userService.calculateAutoDiscount(member, 2)).isZero();
    }

    @Test
    @DisplayName("calculateAutoDiscount: REGULAR boundary value 3 completed cycles gets 5%")
    void calculateAutoDiscount_regularThreeBoundary() {
        Member member = Member.builder().accountType(AccountType.REGULAR).build();

        assertThat(userService.calculateAutoDiscount(member, 3)).isEqualTo(5);
    }

    @Test
    @DisplayName("calculateAutoDiscount: REGULAR with 4-5 completed cycles stays at 5%")
    void calculateAutoDiscount_regularFiveTier() {
        Member member = Member.builder().accountType(AccountType.REGULAR).build();

        assertThat(userService.calculateAutoDiscount(member, 5)).isEqualTo(5);
    }

    @Test
    @DisplayName("calculateAutoDiscount: REGULAR boundary value 6 completed cycles gets 10%")
    void calculateAutoDiscount_regularSixBoundary() {
        Member member = Member.builder().accountType(AccountType.REGULAR).build();

        assertThat(userService.calculateAutoDiscount(member, 6)).isEqualTo(10);
    }

    @Test
    @DisplayName("calculateAutoDiscount: REGULAR with 9 completed cycles stays at 10%")
    void calculateAutoDiscount_regularNineTier() {
        Member member = Member.builder().accountType(AccountType.REGULAR).build();

        assertThat(userService.calculateAutoDiscount(member, 9)).isEqualTo(10);
    }

    @Test
    @DisplayName("calculateAutoDiscount: REGULAR boundary value 10 completed cycles gets 15%")
    void calculateAutoDiscount_regularTenBoundary() {
        Member member = Member.builder().accountType(AccountType.REGULAR).build();

        assertThat(userService.calculateAutoDiscount(member, 10)).isEqualTo(15);
    }

    @Test
    @DisplayName("calculateAutoDiscount: REGULAR with many completed cycles caps at 15%")
    void calculateAutoDiscount_regularCapsAtFifteen() {
        Member member = Member.builder().accountType(AccountType.REGULAR).build();

        assertThat(userService.calculateAutoDiscount(member, 100)).isEqualTo(15);
    }

    @Test
    @DisplayName("calculateAutoDiscount: just-below-boundary values (2, 5, 9) do not yet qualify for the next tier")
    void calculateAutoDiscount_justBelowBoundaries() {
        Member member = Member.builder().accountType(AccountType.REGULAR).build();

        assertThat(userService.calculateAutoDiscount(member, 2)).isZero();
        assertThat(userService.calculateAutoDiscount(member, 5)).isEqualTo(5);
        assertThat(userService.calculateAutoDiscount(member, 9)).isEqualTo(10);
    }

    // ── recalculateDiscount ──────────────────────────────────────────────

    @Test
    @DisplayName("recalculateDiscount: looks up member and completed cycles, then persists the computed discount")
    void recalculateDiscount_success() {
        Member member = Member.builder().id(1L).accountType(AccountType.REGULAR).build();
        when(memberDao.findById(1L)).thenReturn(Optional.of(member));
        when(orderDao.countCompletedByUserId(1L)).thenReturn(6);

        userService.recalculateDiscount(1L);

        verify(memberDao).setDiscount(1L, 10);
    }

    @Test
    @DisplayName("recalculateDiscount: throws RuntimeException when member not found")
    void recalculateDiscount_memberNotFound() {
        when(memberDao.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.recalculateDiscount(404L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("404");

        verify(memberDao, never()).setDiscount(anyLong(), anyInt());
    }

    @Test
    @DisplayName("recalculateDiscount: CORPORATE member with zero completed cycles still gets flat 10%")
    void recalculateDiscount_corporateZeroCycles() {
        Member member = Member.builder().id(2L).accountType(AccountType.CORPORATE).build();
        when(memberDao.findById(2L)).thenReturn(Optional.of(member));
        when(orderDao.countCompletedByUserId(2L)).thenReturn(0);

        userService.recalculateDiscount(2L);

        verify(memberDao).setDiscount(2L, 10);
    }
}