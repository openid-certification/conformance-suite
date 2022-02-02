package net.openid.conformance.raidiam.validators.organisations;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api url: ****
 * Api endpoint: POST /organisations/{OrganisationId}/ess/initiatesigning
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory POST Organisations By OrganisationId Initiatesigning")
public class PostOrganisationsByOrganisationIdInitiatesigningValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public PostOrganisationsByOrganisationIdInitiatesigningValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertOrgTermsAndConditionsDetail(body);

		return environment;
	}
}
