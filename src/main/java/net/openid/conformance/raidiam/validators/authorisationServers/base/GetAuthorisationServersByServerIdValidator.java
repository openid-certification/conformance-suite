package net.openid.conformance.raidiam.validators.authorisationServers.base;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: GET /organisations/{OrganisationId}/authorisationservers/{AuthorisationServerId}
 */
@ApiName("Raidiam Directory GET Authorisation Servers Base by serverId")
public class GetAuthorisationServersByServerIdValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public GetAuthorisationServersByServerIdValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertAuthorisationServers(body);
		return environment;
	}
}
