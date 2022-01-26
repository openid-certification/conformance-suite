package net.openid.conformance.raidiam.validators.referencesAuthorisationDomainRole;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

/**
 * Api url: ****
 * Api endpoint: POST /references/authorisationdomainroles
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory POST Authorisation Domain Role")
public class PostAuthorisationDomainRoleValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertAuthorisationDomainRole(body);
		return environment;
	}

	protected void assertAuthorisationDomainRole(JsonElement body) {
		assertField(body,
			new StringField
				.Builder("AuthorisationDomainName")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("AuthorisationDomainRoleName")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("AuthorisationDomainRoleDescription")
				.setOptional()
				.build());

		assertField(body, CommonFields.getStatus());
	}
}
