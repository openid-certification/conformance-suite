package net.openid.conformance.raidiam.validators.softwareStatements.organisation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: POST /organisations/{OrganisationId}/softwarestatements
 */
@ApiName("Raidiam Directory POST Software Statements for an Organisation")
public class PostStatementsForAnOrganisationValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public PostStatementsForAnOrganisationValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertSoftwareDetails(body);
		return environment;
	}
}
