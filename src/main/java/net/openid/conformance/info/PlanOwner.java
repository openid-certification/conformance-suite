package net.openid.conformance.info;

import net.openid.conformance.statistics.QueryParams;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Map;

/**
 * The account a listing has been asked to narrow to, as {@code owner=<sub>&owner_iss=<iss>}.
 *
 * <p>Both halves, always: a {@code sub} identifies an account only within the issuer that
 * minted it, and this suite has sixteen issuers in production - a listing narrowed to a bare
 * {@code sub} would be narrowed to "everyone with that sub, whoever logged them in". That is a
 * listing oddity, but the same scope drives the bulk delete, where it would be the wrong
 * account's plans being deleted. So a half pair is refused rather than half applied, and this
 * type exists so that a caller cannot hold one.
 *
 * <p>This is deliberately not part of {@link PlanListFilter}: the owner is what <b>scopes</b> a
 * listing, which a filter may never touch - see {@code DBTestPlanService.rejectScopingFields}.
 *
 * @param sub the {@code sub} of the account, as {@code owner.sub} holds it
 * @param iss the issuer that minted that sub, as {@code owner.iss} holds it
 */
public record PlanOwner(String sub, String iss) {

	/**
	 * @param params the request parameters, as {@code HttpServletRequest.getParameterMap()}
	 *               returns them
	 * @return the account asked for, or null if none was
	 * @throws IllegalArgumentException if only one half was sent; the message names both
	 *                                  parameters and is safe to show to the caller
	 */
	public static PlanOwner parse(Map<String, String[]> params) {

		String sub = QueryParams.first(params, "owner");
		String iss = QueryParams.first(params, "owner_iss");

		if (sub == null && iss == null) {
			return null;
		}
		if (sub == null || iss == null) {
			throw new IllegalArgumentException("'owner' and 'owner_iss' go together: a sub identifies "
				+ "an account only within the issuer that minted it, so both are needed to name one");
		}
		return new PlanOwner(sub, iss);
	}

	/** @return criteria matching the plans of this account, and of no other */
	public Criteria toCriteria() {
		return Criteria.where("owner.sub").is(sub).and("owner.iss").is(iss);
	}
}
