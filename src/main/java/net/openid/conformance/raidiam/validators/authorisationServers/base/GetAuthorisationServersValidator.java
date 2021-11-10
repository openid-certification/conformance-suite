package net.openid.conformance.raidiam.validators.authorisationServers.base;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api endpoint: GET /organisations/{OrganisationId}/authorisationservers
 */
@ApiName("Raidiam Directory GET Authorisation Servers Base")
public class GetAuthorisationServersValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public GetAuthorisationServersValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(parts::assertAuthorisationServers)
				.setOptional()
				.build());

		return environment;
	}
}
