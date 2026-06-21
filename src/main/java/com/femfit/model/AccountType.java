package com.femfit.model;

/**
 * Client account type — determines which discount rule applies.
 *
 * <p>REGULAR clients earn a discount automatically based on the number
 * of completed training cycles. CORPORATE clients receive a flat discount
 * tied to the account type itself, regardless of completed cycles. See
 * {@link com.femfit.service.MemberService#calculateAutoDiscount(Member, int)}
 * for the exact rule.</p>
 */
public enum AccountType {
    REGULAR,
    CORPORATE
}