package net.openid.conformance.raidiam.validators.organisationAuthorityClaimsAuthorisation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

/**
 * Api endpoint: POST /organisations/{OrganisationId}/authorityclaims/{OrganisationAuthorityClaimId}/authorisations
 */
@ApiName("Raidiam Directory POST Organisation Authority Claims Authorisations")
public class PostAuthorityClaimsAuthorisationsValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertAuthorityClaims(body);
		return environment;
	}

	protected void assertAuthorityClaims(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("OrganisationAuthorisationId")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("OrganisationAuthorityClaimId")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(body, CommonFields.getStatus());

		assertField(body,
			new StringField
				.Builder("MemberState")
				.setMaxLength(10)
				.setOptional()
				.build());
		}
}
