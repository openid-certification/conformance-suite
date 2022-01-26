package net.openid.conformance.raidiam.validators.authorisationServers.discoveryEndpointsAPI;

import com.google.gson.JsonElement;
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
		JsonElement body = bodyFrom(environment);
		assertEndpoints(body);
		return environment;
	}
}
