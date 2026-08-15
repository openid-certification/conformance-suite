package net.openid.conformance.statistics;

/**
 * The whole-collection counters behind the summary tiles. These are never filtered: they
 * describe the database, not the current slice.
 *
 * @param total      all test runs ever recorded
 * @param totalUsers distinct users who ever ran a test - counted over test runs rather than
 *                   over plans, so that users who only ever ran standalone tests still count
 * @param last24h    runs started in the last 24 hours
 * @param last7d     runs started in the last 7 days
 * @param last30d    runs started in the last 30 days
 * @param inProgress runs currently RUNNING or WAITING
 * @param stuck      non-terminal runs that started more than 24 hours ago
 */
public record TileRow(long total, long totalUsers, long last24h, long last7d, long last30d, long inProgress, long stuck) {
}
