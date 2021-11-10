package net.openid.conformance.raidiam.validators.authorisationServers.base;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: POST /organisations/{OrganisationId}/authorisationservers
 */
@ApiName("Raidiam Directory POST Authorisation Servers Base")
public class PostAuthorisationServersValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public PostAuthorisationServersValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		parts.assertAuthorisationServers(body);
		return environment;
	}
}
