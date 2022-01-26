package net.openid.conformance.raidiam.validators.referencesAuthorityAuthorisationDomain;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

/**
 * Api url: ****
 * Api endpoint: GET /references/authorities/{AuthorityId}/authorisationdomains/{AuthorityAuthorisationDomainId}
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory GET Authority Authorisation Domain By DomainId")
public class GetAuthorityAuthorisationDomainByDomainIdValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertAuthorityAuthorisationDomain(body);
		return environment;
	}

	protected void assertAuthorityAuthorisationDomain(JsonElement body) {
		assertField(body,
			new StringField
				.Builder("AuthorisationDomainName")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("AuthorityId")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("AuthorityAuthorisationDomainId")
				.setOptional()
				.build());

		assertField(body, CommonFields.getStatus());
	}
}
