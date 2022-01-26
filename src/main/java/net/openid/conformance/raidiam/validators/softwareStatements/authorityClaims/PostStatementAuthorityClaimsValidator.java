package net.openid.conformance.raidiam.validators.softwareStatements.authorityClaims;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: POST /organisations/{OrganisationId}/softwarestatements/{SoftwareStatementId}/authorityclaims
 */
@ApiName("Raidiam Directory POST Software Statement Authority Claims")
public class PostStatementAuthorityClaimsValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public PostStatementAuthorityClaimsValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertSoftwareAuthorityClaims(body);
		return environment;
	}
}
