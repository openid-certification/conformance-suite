package net.openid.conformance.raidiam.validators.organisations;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api url: ****
 * Api endpoint: GET /organisations/{OrganisationId}/tnchistory
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Get Organisations By OrganisationId Tnc history")
public class GetOrganisationsByOrganisationIdTnchistoryValidator  extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public GetOrganisationsByOrganisationIdTnchistoryValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		parts.assertDefaultResponseFields(body);

		assertField(body,
			new ObjectArrayField
				.Builder("content")
				.setValidator(parts::assertOrgTermsAndConditionsDetail)
				.setOptional()
				.build());

		return environment;
	}
}
