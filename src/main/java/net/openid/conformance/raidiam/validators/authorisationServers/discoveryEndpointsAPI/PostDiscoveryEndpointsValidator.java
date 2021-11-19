package net.openid.conformance.raidiam.validators.authorisationServers.discoveryEndpointsAPI;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

/**
 * Api endpoint: POST /organisations/{OrganisationId}/authorisationservers/{AuthorisationServerId}/apiresources/{ApiResourceId}/apidiscoveryendpoints
 */
@ApiName("Raidiam Directory POST Authorisation Servers API Discovery Endpoints")
public class PostDiscoveryEndpointsValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertEndpoints(body);
		return environment;
	}

	protected void assertEndpoints(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("ApiDiscoveryId")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("ApiEndpoint")
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				//Pattern is'nt work cos in json example we have this - "ApiEndpoint": "string"
				.setMaxLength(255)
				.setOptional()
				.build());
	}
}
