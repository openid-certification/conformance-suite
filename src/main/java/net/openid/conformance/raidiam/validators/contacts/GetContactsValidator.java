package net.openid.conformance.raidiam.validators.contacts;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api endpoint: GET /organisations/{OrganisationId}/contacts
 */
@ApiName("Raidiam Directory GET Contacts")
public class GetContactsValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public GetContactsValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		parts.assertDefaultResponseFields(body);
		assertField(body,
			new ObjectArrayField
				.Builder("content")
				.setValidator(parts::assertExportContacts)
				.setOptional()
				.build());

		return environment;
	}
}
