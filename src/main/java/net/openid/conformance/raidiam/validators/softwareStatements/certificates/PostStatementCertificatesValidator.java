package net.openid.conformance.raidiam.validators.softwareStatements.certificates;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: POST /organisations/{OrganisationId}/softwarestatements/{SoftwareStatementId}/certificates/{SoftwareStatementCertificateOrKeyType}
 */
@ApiName("Raidiam Directory POST Software Statement Certificates")
public class PostStatementCertificatesValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public PostStatementCertificatesValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertCertificates(body);
		return environment;
	}
}
