package net.openid.conformance.raidiam.validators.organisationCertificates;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

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
		JsonElement body = bodyFrom(environment);
		parts.assertCertificates(body);

		return environment;
	}
}
