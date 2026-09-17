package net.openid.conformance.statistics;

/**
 * A {@link HeatCell} resolved into the coordinates the heatmap is drawn in, plus the period
 * keys the range filter needs. Binning once, when the cube is built, keeps slicing a matter
 * of adding numbers up.
 *
 * @param month {@code YYYY-MM} in UTC
 * @param week  the ISO week Monday in UTC, or null if the day is outside the retained
 *              weekly window
 * @param dow   the day of the week, 0 for Monday through 6 for Sunday
 * @param hour  the hour of the day in UTC, 0 to 23
 * @param runs  runs started in that hour of that day
 */
public record HeatBin(String month, String week, int dow, int hour, long runs) implements Periodic {
}
