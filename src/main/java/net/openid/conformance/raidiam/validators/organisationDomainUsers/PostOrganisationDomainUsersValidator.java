package net.openid.conformance.raidiam.validators.organisationDomainUsers;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api url: ****
 * Api endpoint: POST /organisations/{OrganisationId}/{AuthorisationDomainName}/users
 * Api git hash: ****
 *
 */
public class PostOrganisationDomainUsersValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public PostOrganisationDomainUsersValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		parts.organisationDomainUsersContent(body);
		return environment;
	}
}
