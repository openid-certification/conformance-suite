package net.openid.conformance.raidiam.validators.softwareStatements.authorityClaims;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api endpoint: GET /organisations/{OrganisationId}/softwarestatements/{SoftwareStatementId}/authorityclaims
 */
@ApiName("Raidiam Directory GET Software Statement Authority Claims")
public class GetStatementAuthorityClaimsValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public GetStatementAuthorityClaimsValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(parts::assertSoftwareAuthorityClaims)
				.setOptional()
				.build());

		return environment;
	}
}