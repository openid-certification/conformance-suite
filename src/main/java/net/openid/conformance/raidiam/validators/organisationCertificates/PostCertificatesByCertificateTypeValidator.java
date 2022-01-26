package net.openid.conformance.raidiam.validators.organisationCertificates;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: POST /organisations/{OrganisationId}/certificates/{OrganisationCertificateType}
 *
 */
@ApiName("Raidiam Directory POST Organisation Certificates ByCertificateType")
public class PostCertificatesByCertificateTypeValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public PostCertificatesByCertificateTypeValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertCertificates(body);
		return environment;
	}
}
