package net.openid.conformance.raidiam.validators.organisationCertificates;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api endpoint: GET /organisations/{OrganisationId}/certificates
 *
 */
@ApiName("Raidiam Directory GET Organisation Certificates")
public class GetCertificatesValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public GetCertificatesValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(parts::assertCertificates)
				.build());

		return environment;
	}
}
