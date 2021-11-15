package net.openid.conformance.raidiam.validators.softwareStatements.organisation;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

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
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(parts::assertSoftwareDetails)
				.setOptional()
				.build());

		return environment;
	}
}
