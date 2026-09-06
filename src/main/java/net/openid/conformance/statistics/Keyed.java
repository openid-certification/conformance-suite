package net.openid.conformance.statistics;

/**
 * A cell or tuple narrowed by the variant values and certification profiles behind it.
 * Both are carried as opaque keys — the aggregation groups on them as strings, and
 * {@link StatisticsCube} parses each distinct key once so that slicing never re-parses.
 */
interface Keyed {

	/** @return the key of the variant map this row was aggregated under */
	String variantKey();

	/** @return the key of the certification profile list this row was aggregated under */
	String certKey();
}
