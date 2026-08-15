package net.openid.conformance.statistics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Pattern;

/**
 * The two time buckets the statistics page can be sliced into, and the arithmetic on their
 * period keys.
 *
 * <p>A period key is a string, chosen so that it is both what the client renders and what
 * sorts correctly: {@code YYYY-MM} for a month and the {@code YYYY-MM-DD} of the Monday
 * that starts the ISO week. Because both formats are fixed width and zero padded,
 * comparing two keys of the same granularity with {@link String#compareTo} orders them
 * chronologically, which is what the range clipping and the axis loops rely on.
 *
 * <p>Everything here is UTC: the aggregations derive the keys from the UTC {@code started}
 * string, and {@link #periodOf(LocalDate)} takes a date that the caller has already
 * resolved in UTC.
 */
public enum Granularity {

	/** Calendar months, kept for the whole history. */
	MONTH("month", "YYYY-MM"),

	/** ISO weeks, keyed by their Monday, kept for the trailing window only. */
	WEEK("week", "YYYY-MM-DD");

	private static final Pattern MONTH_KEY = Pattern.compile("\\d{4}-\\d{2}");

	private static final Pattern WEEK_KEY = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

	private final String key;

	private final String format;

	Granularity(String key, String format) {
		this.key = key;
		this.format = format;
	}

	/** @return the name this granularity has on the wire, {@code month} or {@code week} */
	public String key() {
		return key;
	}

	/** @return a human readable description of the period key format, for error messages */
	public String format() {
		return format;
	}

	/**
	 * @param key the wire name
	 * @return the matching granularity
	 * @throws IllegalArgumentException if {@code key} is not one of the wire names; the
	 *         message is meant to be shown to whoever made the request
	 */
	public static Granularity of(String key) {
		for (Granularity granularity : values()) {
			if (granularity.key.equals(key)) {
				return granularity;
			}
		}
		throw new IllegalArgumentException("granularity must be 'month' or 'week', not '" + key + "'");
	}

	/** @return the period key {@code day} falls in: its month, or the Monday of its ISO week */
	public String periodOf(LocalDate day) {
		if (this == MONTH) {
			return YearMonth.from(day).toString();
		}
		return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
	}

	/**
	 * @param period a candidate key
	 * @return true if {@code period} is a key this granularity could have produced - a real
	 *         month for {@link #MONTH}, a real date that is a Monday for {@link #WEEK}
	 */
	public boolean isPeriod(String period) {
		if (this == MONTH) {
			return parseMonth(period) != null;
		}
		LocalDate day = parseDay(period);
		// a week key is always the Monday the week starts on
		return day != null && day.getDayOfWeek() == DayOfWeek.MONDAY;
	}

	/** @return the key of the period immediately after {@code period} */
	public String next(String period) {
		if (this == MONTH) {
			return YearMonth.parse(period).plusMonths(1).toString();
		}
		return LocalDate.parse(period).plusWeeks(1).toString();
	}

	/**
	 * Validates a period key that came from a request, and snaps a weekly one to the Monday
	 * of its ISO week so a client may send any day of the week.
	 *
	 * @param parameter the request parameter the value came from, for the error message
	 * @param value     the value to validate
	 * @return the normalised key
	 * @throws IllegalArgumentException if the value is not a key of this granularity
	 */
	public String normalisePeriod(String parameter, String value) {
		if (this == MONTH) {
			YearMonth month = parseMonth(value);
			if (month != null) {
				return month.toString();
			}
		} else {
			LocalDate day = parseDay(value);
			if (day != null) {
				return periodOf(day);
			}
		}
		throw new IllegalArgumentException("%s must be a %s in %s format, not '%s'"
			.formatted(parameter, this == MONTH ? "month" : "date", format, value));
	}

	/** @return the month, or null if {@code period} is not a usable {@code YYYY-MM} key */
	private static YearMonth parseMonth(String period) {
		if (period == null || !MONTH_KEY.matcher(period).matches()) {
			return null;
		}
		try {
			return YearMonth.parse(period);
		} catch (DateTimeParseException e) {
			return null; // the right shape but not a real month, e.g. 2026-13
		}
	}

	/** @return the date, or null if {@code period} is not a usable {@code YYYY-MM-DD} key */
	private static LocalDate parseDay(String period) {
		if (period == null || !WEEK_KEY.matcher(period).matches()) {
			return null;
		}
		try {
			return LocalDate.parse(period);
		} catch (DateTimeParseException e) {
			return null; // the right shape but not a real date, e.g. 2026-02-30
		}
	}
}
