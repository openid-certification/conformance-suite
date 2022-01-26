package net.openid.conformance.raidiam.validators.organisationDomainUsers;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api url: ****
 * Api endpoint: POST /organisations/{OrganisationId}/{AuthorisationDomainName}/users
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory POST Organisation Domain Users")
public class PostOrganisationDomainUsersValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public PostOrganisationDomainUsersValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.organisationDomainUsersContent(body);
		return environment;
	}
}
