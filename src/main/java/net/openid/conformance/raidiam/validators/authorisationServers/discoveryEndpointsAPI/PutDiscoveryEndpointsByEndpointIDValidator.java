package net.openid.conformance.raidiam.validators.authorisationServers.discoveryEndpointsAPI;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: POST /organisations/{OrganisationId}/authorisationservers/{AuthorisationServerId}/apiresources/{ApiResourceId}/apidiscoveryendpoints/{ApiDiscoveryEndpointId}
 */
@ApiName("Raidiam Directory PUT Authorisation Servers API Discovery Endpoints")
public class PutDiscoveryEndpointsByEndpointIDValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public PutDiscoveryEndpointsByEndpointIDValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		parts.assertApiResources(body);
		return environment;
	}
}
