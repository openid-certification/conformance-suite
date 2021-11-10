package net.openid.conformance.raidiam.validators.contacts;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
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
		JsonObject body = bodyFrom(environment);
		parts.assertExportContacts(body);
		return environment;
	}
}
