package net.openid.conformance.raidiam.validators.softwareStatements.authorityClaims;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
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
		JsonObject body = bodyFrom(environment);
		parts.assertSoftwareAuthorityClaims(body);
		return environment;
	}
}
