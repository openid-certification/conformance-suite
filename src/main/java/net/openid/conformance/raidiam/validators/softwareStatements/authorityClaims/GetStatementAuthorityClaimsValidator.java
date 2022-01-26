package net.openid.conformance.raidiam.validators.softwareStatements.authorityClaims;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: GET /organisations/{OrganisationId}/softwarestatements/{SoftwareStatementId}/authorityclaims
 */
@ApiName("Raidiam Directory GET Software Statement Authority Claims")
public class GetStatementAuthorityClaimsValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public GetStatementAuthorityClaimsValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertSoftwareAuthorityClaims(body);
		return environment;
	}
}
