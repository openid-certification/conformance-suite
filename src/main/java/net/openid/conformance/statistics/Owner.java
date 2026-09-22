package net.openid.conformance.statistics;

/**
 * The account a test run belongs to, as {@code TEST_INFO.owner} stores it. A {@code sub}
 * names an account only within its issuer, so the two always travel together.
 *
 * @param iss the issuer that authenticated the user
 * @param sub the user's subject at that issuer
 */
public record Owner(String iss, String sub) {
}
