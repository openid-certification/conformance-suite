package net.openid.conformance.raidiam.validators.authorisationServers.resourcesAPI;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api endpoint: GET /organisations/{OrganisationId}/authorisationservers/{AuthorisationServerId}/apiresources
 */
@ApiName("Raidiam Directory GET Authorisation Servers API Resources")
public class GetResourceValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public GetResourceValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField
				.Builder("$")
				.setValidator(parts::assertApiResources)
				.setOptional()
				.build());

		return environment;
	}
}
