package net.openid.conformance.raidiam.validators.organisationAuthorityClaims;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

/**
 * Api endpoint: POST /organisations/{OrganisationId}/authorityclaims
 *
 */
@ApiName("Raidiam Directory POST Authority Claims")
public class PostAuthorityClaimsValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public PostAuthorityClaimsValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		parts.assertOrgDomainRoleClaims(body);
		return environment;
	}
}
