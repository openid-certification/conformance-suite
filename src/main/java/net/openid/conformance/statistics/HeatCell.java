package net.openid.conformance.statistics;

/**
 * Test runs started on one day in one hour, as the heatmap aggregation produces them.
 *
 * <p>Both keys are the raw substrings of the UTC {@code started} string, so a document with
 * an unusable {@code started} yields something that does not parse; {@link StatisticsCube}
 * drops those when it bins the cells into {@link HeatBin}s.
 *
 * @param day  {@code YYYY-MM-DD} in UTC
 * @param hour {@code HH} in UTC, zero padded
 * @param runs runs started in that hour of that day
 */
public record HeatCell(String day, String hour, long runs) {
}
