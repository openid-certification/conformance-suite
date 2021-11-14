package net.openid.conformance.raidiam.validators.authorisationServers.discoveryEndpointsAPI;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * This class corresponds to {@link PostDiscoveryEndpointsValidator}
 * Api endpoint: GET /organisations/{OrganisationId}/authorisationservers/{AuthorisationServerId}/apiresources/{ApiResourceId}/apidiscoveryendpoints
 */
@ApiName("Raidiam Directory GET Authorisation Servers API Discovery Endpoints")
public class GetDiscoveryEndpointsValidator extends PostDiscoveryEndpointsValidator {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertEndpoints)
				.setOptional()
				.build());

		return environment;
	}
}
