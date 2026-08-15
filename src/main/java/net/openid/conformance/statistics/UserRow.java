package net.openid.conformance.statistics;

import java.util.List;

/**
 * The months in which one user ran at least one test.
 *
 * @param iss    the issuer of the user's identity; rows without one are ignored
 * @param sub    the subject of the user's identity; rows without one are ignored
 * @param months the {@code YYYY-MM} keys the user was active in, in no particular order
 */
public record UserRow(String iss, String sub, List<String> months) {
}
