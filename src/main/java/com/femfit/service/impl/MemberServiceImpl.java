package com.femfit.service.impl;

import com.femfit.dao.MemberDao;
import com.femfit.dao.OrderDao;
import com.femfit.dto.PageDto;
import com.femfit.dto.RegisterDto;
import com.femfit.exception.EmailAlreadyTakenException;
import com.femfit.exception.InvalidPasswordException;
import com.femfit.model.AccountType;
import com.femfit.model.Role;
import com.femfit.model.Member;
import com.femfit.service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of {@link MemberService}.
 *
 * <p>Design patterns:
 * <ul>
 *   <li><strong>Strategy</strong> — PasswordEncoder injected and swappable.</li>
 *   <li><strong>DAO</strong> — delegates DB access to {@link MemberDao}.</li>
 * </ul>
 */
@Service
public class MemberServiceImpl implements MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceImpl.class);

    private static final long   MAX_AVATAR_SIZE  = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private final MemberDao memberDao;
    private final OrderDao  orderDao;
    private final PasswordEncoder passwordEncoder;

    /** Absolute path to Tomcat webapps/femfit — injected from application.properties */
    @Value("${app.upload.base-path:#{null}}")
    private String uploadBasePath;

    @Autowired
    public MemberServiceImpl(MemberDao memberDao, OrderDao orderDao,
                             PasswordEncoder passwordEncoder) {
        this.memberDao       = memberDao;
        this.orderDao        = orderDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Member register(RegisterDto dto) {
        log.info("Registering new user: {}", dto.getEmail());
        if (memberDao.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyTakenException("Email already registered: " + dto.getEmail());
        }
        Member member = Member.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .birthDate(dto.getBirthDate())
                .role(Role.CLIENT)
                .active(true)
                .accountType(AccountType.REGULAR)
                .build();
        Member saved = memberDao.save(member);
        log.info("User registered: id={}", saved.getId());
        return saved;
    }

    @Override public Optional<Member> findById(Long id)            { return memberDao.findById(id); }
    @Override public Optional<Member> findByEmail(String email)    { return memberDao.findByEmail(email); }

    @Override
    public PageDto<Member> findByRole(Role role, int page, int size) {
        int offset = (page - 1) * size;
        List<Member> members = memberDao.findByRole(role, offset, size);
        int total = memberDao.countByRole(role);
        return new PageDto<>(members, page, size, total);
    }

    @Override public void updateProfile(Member member) { memberDao.update(member); }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        Member member = memberDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        if (!passwordEncoder.matches(oldPassword, member.getPasswordHash())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
        memberDao.updatePassword(userId, passwordEncoder.encode(newPassword));
    }

    @Override public void setActive(Long userId, boolean isActive) { memberDao.setActive(userId, isActive); }

    @Override
    public void setDiscount(Long userId, int discountPercent) {
        if (discountPercent < 0 || discountPercent > 100)
            throw new IllegalArgumentException("Discount must be 0-100");
        memberDao.setDiscount(userId, discountPercent);
    }

    @Override
    public void setAccountType(Long userId, AccountType accountType) {
        memberDao.setAccountType(userId, accountType);
        recalculateDiscount(userId);
    }

    @Override
    public int calculateAutoDiscount(Member member, int completedCycles) {
        if (member.getAccountType() == AccountType.CORPORATE) return 10;
        if (completedCycles >= 10) return 15;
        if (completedCycles >= 6)  return 10;
        if (completedCycles >= 3)  return 5;
        return 0;
    }

    @Override
    public void recalculateDiscount(Long userId) {
        Member member = memberDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        int completedCycles = orderDao.countCompletedByUserId(userId);
        int newDiscount = calculateAutoDiscount(member, completedCycles);
        memberDao.setDiscount(userId, newDiscount);
    }

    // ── Avatar upload ──────────────────────────────────────────────────────

    @Override
    public String uploadAvatar(Long memberId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Avatar must be JPEG, PNG or WEBP");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("Avatar file must be under 5 MB");
        }

        // Determine save directory.
        // Priority: app.upload.base-path property (set in application.properties)
        // Fallback: servlet context real path via System property set by WebAppInitializer
        String basePath = uploadBasePath;
        if (basePath == null || basePath.isBlank()) {
            basePath = System.getProperty("app.webapp.path", "");
        }
        if (basePath == null || basePath.isBlank()) {
            throw new RuntimeException(
                "Upload path not configured. Set app.upload.base-path in application.properties.");
        }

        try {
            Path avatarDir = Paths.get(basePath, "static", "img", "avatars");
            Files.createDirectories(avatarDir);

            String ext = switch (contentType) {
                case "image/png"  -> ".png";
                case "image/webp" -> ".webp";
                default           -> ".jpg";
            };
            String filename = memberId + ext;
            Path dest = avatarDir.resolve(filename);
            file.transferTo(dest.toFile());

            String relUrl = "/femfit/static/img/avatars/" + filename;
            memberDao.updateAvatarUrl(memberId, relUrl);
            log.info("Avatar saved: memberId={}, path={}", memberId, dest);
            return relUrl;

        } catch (IOException e) {
            log.error("Failed to save avatar for member {}: {}", memberId, e.getMessage());
            throw new RuntimeException("Failed to save avatar file", e);
        }
    }
}
