package net.openid.conformance.raidiam.validators.organisations;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;

/**
 * Api url: ****
 * Api endpoint: GET /organisations/{OrganisationId}
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Get Organisations By OrganisationId")
public class GetOrganisationsByOrganisationIdValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public GetOrganisationsByOrganisationIdValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new ObjectField
				.Builder("OrgDetails")
				.setValidator(parts::organisationContent)
				.setOptional()
				.build());

		assertField(body,
			new ObjectField
				.Builder("TncDetails")
				.setValidator(this::assertTncDetails)
				.setOptional()
				.build());

		return environment;
	}

	private void assertTncDetails(JsonObject tncDetails) {
		assertField(tncDetails,
			new BooleanField
				.Builder("TnCSigned")
				.setOptional()
				.build());

		assertField(tncDetails,
			new BooleanField
				.Builder("TnCUpdated")
				.setOptional()
				.build());

		assertField(tncDetails,
			new ObjectArrayField
				.Builder("TnCsToBeSigned")
				.setValidator(parts::assertTermsAndConditionsItem)
				.setOptional()
				.build());
	}
}
