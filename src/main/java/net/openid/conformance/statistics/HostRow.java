package net.openid.conformance.statistics;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One external server the suite has been pointed at over the trailing
 * {@value StatisticsCube#MODULE_MONTHS} months. Passed straight through to the client, so
 * the timestamp is a string.
 *
 * @param host     the host part of the configured issuer, discovery, credential issuer or
 *                 entity identifier URL, lower-cased
 * @param runs     test runs configured against it
 * @param users    distinct users who ran those tests
 * @param lastSeen when it was last used, ISO-8601 UTC
 */
@Schema(name = "StatisticsExternalHost")
public record HostRow(String host, long runs, long users, String lastSeen) {
}
