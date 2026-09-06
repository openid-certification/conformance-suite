package net.openid.conformance.statistics;

/**
 * Test module runs for one period, plan, plan level variant and certification profile: the
 * fact table behind the runs and results charts.
 *
 * <p>A cell coming out of the aggregation carries both keys. After
 * {@link StatisticsCube} has rolled the cells up, exactly one of them is set: monthly cells
 * have a {@code month} and no {@code week}, weekly cells a {@code week} and no {@code month}
 * (a week can straddle two months, so a rolled up weekly cell has no single month).
 *
 * @param month      {@code YYYY-MM} in UTC, or null on a weekly cell
 * @param week       the {@code YYYY-MM-DD} Monday of the ISO week in UTC, or null on a
 *                   monthly cell and on runs whose {@code started} could not be parsed
 * @param planName   the plan the runs belong to, or null for standalone runs and for runs
 *                   whose plan document no longer exists
 * @param standalone true if the runs were not part of a test plan
 * @param variantKey the plan level variant, canonicalised by {@link VariantKeys}; empty if none
 * @param certKey    the plan's certification profile names as {@link CertKeys} joins them; empty if none
 * @param runs       total runs in the cell
 * @param passed     runs whose result was PASSED
 * @param failed     runs whose result was FAILED
 * @param warning    runs whose result was WARNING
 * @param review     runs whose result was REVIEW
 * @param skipped    runs whose result was SKIPPED
 */
public record RunCell(String month, String week, String planName, boolean standalone, String variantKey,
	String certKey, long runs, long passed, long failed, long warning, long review, long skipped)
	implements Periodic, Keyed {
}
