package net.openid.conformance.raidiam.validators.referencesAuthorisationDomainRole;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
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
		JsonObject body = bodyFrom(environment);
		assertAuthorisationDomainRole(body);
		return environment;
	}

	protected void assertAuthorisationDomainRole(JsonObject body) {
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
