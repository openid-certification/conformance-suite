package net.openid.conformance.raidiam.validators.contacts;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api endpoint: POST /organisations/{OrganisationId}/contacts
 */
@ApiName("Raidiam Directory POST Contacts")
public class PostContactsValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public PostContactsValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertExportContacts(body);
		return environment;
	}
}
