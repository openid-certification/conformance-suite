package net.openid.conformance.raidiam.validators.organisationAuthorityClaims;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: POST /organisations/{OrganisationId}/authorityclaims
 *
 */
@ApiName("Raidiam Directory POST Authority Claims")
public class PostAuthorityClaimsValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public PostAuthorityClaimsValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertOrgDomainRoleClaims(body);
		return environment;
	}
}
