package net.openid.conformance.raidiam.validators.authorisationServers.discoveryEndpointsAPI;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This class corresponds to {@link PostDiscoveryEndpointsValidator}
 * Api endpoint: GET /organisations/{OrganisationId}/authorisationservers/{AuthorisationServerId}/apiresources/{ApiResourceId}/apidiscoveryendpoints/{ApiDiscoveryEndpointId}
 */
@ApiName("Raidiam Directory GET Authorisation Servers by EndpointID Ser API Discovery Endpoints")
public class GetDiscoveryEndpointsByEndpointIDValidator extends PostDiscoveryEndpointsValidator {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = initBodyArray(environment);
		assertEndpoints(body);
		return environment;
	}
}
