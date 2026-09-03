package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * As {@link ValidateDisclosedClaimsMatchDcqlQuery}, but exempts the picture (portrait) claim from
 * the check. Callers pair this with {@link EnsurePidPictureClaimDisclosed} invoked at WARNING
 * severity.
 *
 * This is used with a query whose claim_sets offer the full claim list and a fallback without the
 * picture, so the claims required under every claim set option — everything except the picture —
 * are enforced here, and the picture (present only when the wallet can satisfy the preferred
 * claim set) is checked separately. This sidesteps the flatten-all-claims TODO in
 * {@link ValidateDisclosedClaimsMatchDcqlQuery} for this specific query shape, since the required
 * set computed here equals the intersection of the claim set options.
 */
public class ValidateDisclosedClaimsMatchDcqlQueryExceptPicture extends ValidateDisclosedClaimsMatchDcqlQuery {

	@Override
	protected Set<List<String>> requiredClaimPaths(JsonObject matchingCredential) {
		Set<List<String>> paths = new HashSet<>(super.requiredClaimPaths(matchingCredential));
		paths.remove(List.of("picture"));
		return paths;
	}
}
