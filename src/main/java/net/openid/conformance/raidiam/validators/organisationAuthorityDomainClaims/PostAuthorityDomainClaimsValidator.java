package net.openid.conformance.raidiam.validators.organisationAuthorityDomainClaims;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

/**
 * Api url: ****
 * Api endpoint: POST /organisations/{OrganisationId}/authoritydomainclaims
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory POST Authority Domain Claims")
public class PostAuthorityDomainClaimsValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertAuthorityDomainClaims(body);
		return environment;
	}

	protected void assertAuthorityDomainClaims(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("OrganisationAuthorityDomainClaimId")
				.setOptional()
				.build());

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
				.Builder("AuthorityName")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("RegistrationId")
				.setOptional()
				.build());

		assertField(body, CommonFields.getStatus());
	}
}
