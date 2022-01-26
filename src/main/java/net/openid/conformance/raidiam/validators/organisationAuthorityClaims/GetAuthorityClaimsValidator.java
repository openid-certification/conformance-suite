package net.openid.conformance.raidiam.validators.organisationAuthorityClaims;

import com.google.gson.JsonElement;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: GET /organisations/{OrganisationId}/authorityclaims
 *
 */
@ApiName("Raidiam Directory GET Authority Claims")
public class GetAuthorityClaimsValidator extends PostAuthorityClaimsValidator {

	private final CommonParts parts;
	public GetAuthorityClaimsValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertOrgDomainRoleClaims(body);
		return environment;
	}
}
