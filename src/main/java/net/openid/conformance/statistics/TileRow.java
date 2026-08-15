package net.openid.conformance.statistics;

/**
 * The whole-collection counters behind the summary tiles.
 *
 * @param total      all test runs ever recorded
 * @param last24h    runs started in the last 24 hours
 * @param last7d     runs started in the last 7 days
 * @param last30d    runs started in the last 30 days
 * @param inProgress runs currently RUNNING or WAITING
 * @param stuck      non-terminal runs that started more than 24 hours ago
 */
public record TileRow(long total, long last24h, long last7d, long last30d, long inProgress, long stuck) {
}
