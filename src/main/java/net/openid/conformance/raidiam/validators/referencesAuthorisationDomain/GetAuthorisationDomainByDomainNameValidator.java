package net.openid.conformance.raidiam.validators.referencesAuthorisationDomain;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

/**
 * Api url: ****
 * Api endpoint: GET /references/authorisationdomains/{AuthorisationDomainName}
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Get Authorisation Domain By DomainName")
public class GetAuthorisationDomainByDomainNameValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertAuthorisationDomain(body);
		return environment;
	}

	protected void assertAuthorisationDomain(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("AuthorisationDomainName")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("AuthorisationDomainRegion")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("AuthorisationDomainDescription")
				.setOptional()
				.build());

		assertField(body, CommonFields.getStatus());
	}
}
