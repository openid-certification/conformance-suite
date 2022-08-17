package net.openid.conformance.raidiam.validators.softwareStatements.certificates;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api endpoint: GET /organisations/{OrganisationId}/softwarestatements/{SoftwareStatementId}/certificates/{SoftwareStatementCertificateOrKeyType}
 */
@ApiName("Raidiam Directory GET Software Statement Certificates")
public class GetStatementCertificatesValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public GetStatementCertificatesValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField
				.Builder("$")
				.setValidator(parts::assertCertificates)
				.setOptional()
				.build());

		return environment;
	}
}
