package net.openid.conformance.raidiam.validators.softwareStatements.organisation;

import com.google.gson.JsonElement;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: GET /organisations/{OrganisationId}/softwarestatements
 */
@ApiName("Raidiam Directory GET Software Statements for an Organisation")
public class GetStatementsForAnOrganisationValidator extends PostStatementsForAnOrganisationValidator {

	private final CommonParts parts;
	public GetStatementsForAnOrganisationValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertSoftwareDetails(body);
		return environment;
	}
}
