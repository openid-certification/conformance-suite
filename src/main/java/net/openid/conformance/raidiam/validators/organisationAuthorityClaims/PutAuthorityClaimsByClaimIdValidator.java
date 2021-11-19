package net.openid.conformance.raidiam.validators.organisationAuthorityClaims;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: PUT /organisations/{OrganisationId}/authorityclaims/{OrganisationAuthorityClaimId}
 *
 */
@ApiName("Raidiam Directory PUT Authority Claims by ClaimId")
public class PutAuthorityClaimsByClaimIdValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public PutAuthorityClaimsByClaimIdValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		parts.assertOrgDomainRoleClaims(body);
		return environment;
	}
}
