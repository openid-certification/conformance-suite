package net.openid.conformance.statistics;

import java.util.List;

/**
 * The periods one user created test plans of one plan, plan level variant and certification
 * profile in: the fact table behind the users chart and the distinct user counts.
 *
 * <p>Users are counted on a <b>plan basis</b> - a user is active in the period they created
 * a plan - which is what makes the users series filterable by plan, variant and
 * certification profile at all. The whole-database {@code totalUsers} tile stays counted
 * over test runs, see {@link TileRow}.
 *
 * @param planName   the plan name, or null if the document has none
 * @param variantKey the plan level variant, canonicalised by {@link VariantKeys}; empty if none
 * @param certKey    the plan's certification profile names as {@link CertKeys} joins them; empty if none
 * @param ownerId    an id standing in for the user's {@code iss} and {@code sub}, compact
 *                   because the only thing ever done with it is counting distinct values
 * @param months     the {@code YYYY-MM} keys this tuple was active in
 * @param weeks      the ISO week Monday keys this tuple was active in; {@link StatisticsCube}
 *                   drops the ones outside the retained window
 * @param activeBeforeWindow whether the tuple was active in a week older than the retained
 *                   window: what {@code weeks} can no longer say once those weeks are gone,
 *                   and what {@code months} cannot say either when the window opens mid
 *                   month
 */
public record UserTuple(String planName, String variantKey, String certKey, int ownerId,
	List<String> months, List<String> weeks, boolean activeBeforeWindow) implements Keyed {

	/** @return the periods this tuple was active in at {@code granularity} */
	public List<String> periods(Granularity granularity) {
		return granularity == Granularity.MONTH ? months : weeks;
	}
}
