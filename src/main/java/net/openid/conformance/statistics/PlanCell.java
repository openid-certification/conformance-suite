package net.openid.conformance.statistics;

/**
 * Test plans created in one period for one plan, plan level variant and certification
 * profile: the fact table behind the plans and certification charts.
 *
 * <p>As with {@link RunCell}, exactly one of {@code month} and {@code week} is set once
 * {@link StatisticsCube} has rolled the cells up.
 *
 * @param month      {@code YYYY-MM} in UTC, or null on a weekly cell
 * @param week       the {@code YYYY-MM-DD} Monday of the ISO week in UTC, or null on a
 *                   monthly cell and on plans whose {@code started} could not be parsed
 * @param planName   the plan name, or null if the document has none
 * @param variantKey the plan level variant, canonicalised by {@link VariantKeys}; empty if none
 * @param certKey    the plan's certification profile names as {@link CertKeys} joins them; empty if none
 * @param plans      plans created in the cell
 * @param certified  plans that were made immutable, which is what downloading a
 *                   certification package does
 * @param published  plans that were published, i.e. have a {@code publish} setting
 */
public record PlanCell(String month, String week, String planName, String variantKey, String certKey,
	long plans, long certified, long published) implements Periodic {
}
