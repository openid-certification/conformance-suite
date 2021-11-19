package net.openid.conformance.raidiam.validators.organisationsExport;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api url: ****
 * Api endpoint: GET /organisations/mock
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Get Organisations Export Mock")
public class GetOrganisationsExportMockValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public GetOrganisationsExportMockValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		parts.organisationContent(body);
		return environment;
	}
}
