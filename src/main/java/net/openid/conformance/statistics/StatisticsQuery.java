package net.openid.conformance.statistics;

import java.util.Map;

/**
 * What the client is asking to see: the granularity of the axis, the range of it, and the
 * filters applied to every series.
 *
 * <p>Only the request parameters this understands are read; anything else in the map (the
 * {@code refresh} flag, a cache buster) is ignored. Anything it does understand but cannot
 * use is rejected with a message meant for the person who made the request, which the API
 * turns into a 400.
 *
 * @param granularity {@code month} or {@code week}; monthly by default
 * @param from        the first period to show, inclusive, or null for as far back as there
 *                    is data
 * @param to          the last period to show, inclusive, or null for up to today
 * @param family      only count cells of this spec family, or null for all of them
 * @param plan        only count cells of this test plan, or null for all of them
 * @param variant     only count cells whose plan level variant has all of these parameters
 *                    set to these values; empty for all of them
 * @param cert        only count cells with exactly this certification profile, or null for
 *                    all of them
 */
public record StatisticsQuery(Granularity granularity, String from, String to, String family, String plan,
	Map<String, String> variant, String cert) {

	public StatisticsQuery {
		variant = variant == null ? Map.of() : Map.copyOf(variant);
	}

	/** @return the whole history, by month, unfiltered */
	public static StatisticsQuery defaults() {
		return new StatisticsQuery(Granularity.MONTH, null, null, null, null, Map.of(), null);
	}

	/**
	 * @param params the request parameters, as
	 *               {@code HttpServletRequest.getParameterMap()} returns them
	 * @return the query they describe; blank values count as absent, so a client can send
	 *         an empty parameter for "no filter"
	 * @throws IllegalArgumentException if a value cannot be used; the message names the
	 *                                  parameter and is safe to show to the caller
	 */
	public static StatisticsQuery parse(Map<String, String[]> params) {
		String requested = QueryParams.first(params, "granularity");
		Granularity granularity = requested == null ? Granularity.MONTH : Granularity.of(requested);
		String from = period(granularity, "from", QueryParams.first(params, "from"));
		String to = period(granularity, "to", QueryParams.first(params, "to"));
		if (from != null && to != null && from.compareTo(to) > 0) {
			throw new IllegalArgumentException("from (%s) must not be after to (%s)".formatted(from, to));
		}
		return new StatisticsQuery(granularity, from, to, QueryParams.first(params, "family"),
			QueryParams.first(params, "plan"), QueryParams.variant(params), QueryParams.first(params, "cert"));
	}

	private static String period(Granularity granularity, String parameter, String value) {
		return value == null ? null : granularity.normalisePeriod(parameter, value);
	}

}
