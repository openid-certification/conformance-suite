package net.openid.conformance.raidiam.validators.authorisationServers.resourcesAPI;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: GET /organisations/{OrganisationId}/authorisationservers/{AuthorisationServerId}/apiresources/{ApiResourceId}
 */
@ApiName("Raidiam Directory GET Authorisation Servers API Resources by ResourceId")
public class GetResourceByResourceIdValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public GetResourceByResourceIdValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = initBodyArray(environment);
		parts.assertApiResources(body);
		return environment;
	}
}
