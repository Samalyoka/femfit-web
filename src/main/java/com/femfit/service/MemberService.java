package com.femfit.service;

import com.femfit.dto.RegisterDto;
import com.femfit.dto.PageDto;
import com.femfit.model.AccountType;
import com.femfit.model.Role;
import com.femfit.model.Member;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Service interface for member-related business logic.
 */
public interface MemberService {

    Member register(RegisterDto dto);
    Optional<Member> findById(Long id);
    Optional<Member> findByEmail(String email);
    PageDto<Member> findByRole(Role role, int page, int size);
    void updateProfile(Member member);
    void changePassword(Long memberId, String oldPassword, String newPassword);
    void setActive(Long memberId, boolean isActive);
    void setDiscount(Long memberId, int discountPercent);
    void setAccountType(Long memberId, AccountType accountType);
    int calculateAutoDiscount(Member member, int completedCycles);
    void recalculateDiscount(Long memberId);

    /**
     * Saves the uploaded avatar file to disk and updates avatar_url in DB.
     * Allowed formats: JPEG, PNG, WEBP. Max size: 5MB.
     *
     * @param memberId the member's id
     * @param file     the uploaded multipart file
     * @return the saved relative URL, e.g. /static/img/avatars/42.jpg
     */
    String uploadAvatar(Long memberId, MultipartFile file);
}
