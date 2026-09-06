package net.openid.conformance.statistics;

/**
 * A cell that carries a key for each granularity, of which the one being sliced at is read.
 * Exactly one of the two is set once {@link StatisticsCube} has rolled the cells up.
 */
interface Periodic {

	/** @return {@code YYYY-MM} in UTC, or null on a weekly cell */
	String month();

	/** @return the {@code YYYY-MM-DD} Monday of the ISO week in UTC, or null on a monthly cell */
	String week();

	/** @return the key of the period this cell belongs to at {@code granularity} */
	default String period(Granularity granularity) {
		return granularity == Granularity.MONTH ? month() : week();
	}
}
