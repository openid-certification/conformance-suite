package net.openid.conformance.statistics;

/**
 * The whole-collection counters behind the summary tiles. These are never filtered: they
 * describe the database, not the current slice.
 *
 * <p>Everything but {@code total} and {@code totalUsers} is counted over the runs of the
 * last year, so that the scan behind them is a range of the {@code {started: 1}} index
 * rather than a pass over every test ever run; see {@link MongoStatisticsSource#tiles}.
 *
 * @param total      all test runs ever recorded, as the collection's own document count -
 *                   an estimate, which can be a little stale after an unclean shutdown or
 *                   while a chunk is being migrated
 * @param totalUsers distinct users who ever created a test plan. Counted over the plans
 *                   rather than over the runs, which is far cheaper and the same basis as
 *                   the active users series; somebody who has only ever run standalone
 *                   tests is therefore not counted
 * @param last24h    runs started in the last 24 hours
 * @param last7d     runs started in the last 7 days
 * @param last30d    runs started in the last 30 days
 * @param inProgress runs of the last year currently RUNNING or WAITING
 * @param stuck      runs of the last year that are non-terminal and started more than 24
 *                   hours ago
 */
public record TileRow(long total, long totalUsers, long last24h, long last7d, long last30d, long inProgress, long stuck) {
}
