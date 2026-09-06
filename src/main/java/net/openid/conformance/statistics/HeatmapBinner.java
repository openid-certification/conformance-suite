package net.openid.conformance.statistics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Resolves the day and hour keys of the heatmap aggregation into day-of-week and hour
 * coordinates, and adds the bins up into the 7 x 24 grid the client draws.
 *
 * <p>Everything is UTC, including the day of the week: the aggregation derives the keys
 * from the UTC {@code started} string and no time zone is applied anywhere.
 */
final class HeatmapBinner {

	/** Rows of the grid: Monday first, as the client renders it. */
	private static final int DAYS = 7;

	/** Columns of the grid: hours of the day. */
	private static final int HOURS = 24;

	private static final int HOUR_KEY_LENGTH = 2;

	private HeatmapBinner() {
	}

	/**
	 * @param cells      the raw cells, one per day and hour
	 * @param oldestWeek the first week of the retained weekly window; bins older than that
	 *                   keep their month but lose their week, exactly like the run cells
	 * @return one bin per usable cell; cells whose day or hour does not parse are dropped
	 */
	static List<HeatBin> bin(List<HeatCell> cells, String oldestWeek) {
		List<HeatBin> bins = new ArrayList<>(cells.size());
		for (HeatCell cell : cells) {
			LocalDate day = Granularity.parseDay(cell.day());
			int hour = parseHour(cell.hour());
			if (day == null || hour < 0) {
				continue;
			}
			String week = Granularity.WEEK.periodOf(day);
			bins.add(new HeatBin(Granularity.MONTH.periodOf(day),
				week.compareTo(oldestWeek) >= 0 ? week : null,
				day.getDayOfWeek().getValue() - 1, hour, cell.runs()));
		}
		return List.copyOf(bins);
	}

	/**
	 * @param bins        every bin in the cube
	 * @param granularity the granularity {@code periods} are keyed by
	 * @param periods     the periods currently on the axis; bins outside them are ignored
	 * @return 7 rows of 24 counts, Monday first, hour 0 first; always fully populated
	 */
	static List<List<Long>> heatmap(List<HeatBin> bins, Granularity granularity, Set<String> periods) {
		long[][] counts = new long[DAYS][HOURS];
		for (HeatBin bin : bins) {
			String period = bin.period(granularity);
			if (period == null || !periods.contains(period)) {
				continue;
			}
			counts[bin.dow()][bin.hour()] += bin.runs();
		}
		List<List<Long>> grid = new ArrayList<>(DAYS);
		for (long[] row : counts) {
			List<Long> hours = new ArrayList<>(HOURS);
			for (long count : row) {
				hours.add(count);
			}
			grid.add(List.copyOf(hours));
		}
		return List.copyOf(grid);
	}

	/** @return the hour, or -1 if the key is not a usable {@code HH} hour */
	private static int parseHour(String hour) {
		if (hour == null || hour.length() != HOUR_KEY_LENGTH) {
			return -1;
		}
		try {
			int parsed = Integer.parseInt(hour);
			return parsed >= 0 && parsed < HOURS ? parsed : -1;
		} catch (NumberFormatException e) {
			return -1; // a document whose started field is missing or not a date
		}
	}
}
