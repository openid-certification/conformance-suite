package net.openid.conformance.raidiam.validators.organisationAuthorityClaims;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 *AuthorityClaims
 * Api endpoint: GET /organisations/{OrganisationId}/authorityclaims/{OrganisationAuthorityClaimId}
 *
 */
@ApiName("Raidiam Directory GET Authority Claims by ClaimId")
public class GetAuthorityClaimsByClaimIdValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public GetAuthorityClaimsByClaimIdValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		parts.assertOrgDomainRoleClaims(body);
		return environment;
	}
}
