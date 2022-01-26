package net.openid.conformance.raidiam.validators.authorisationServers.resourcesAPI;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: POST /organisations/{OrganisationId}/authorisationservers/{AuthorisationServerId}/apiresources
 */
@ApiName("Raidiam Directory POST Authorisation Servers API Resources")
public class PostResourceValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public PostResourceValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertApiResources(body);
		return environment;
	}
}
