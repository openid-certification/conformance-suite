package net.openid.conformance.raidiam.validators.organisationAuthorityClaims;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api endpoint: GET /organisations/{OrganisationId}/authorityclaims
 *
 */
@ApiName("Raidiam Directory GET Authority Claims")
public class GetAuthorityClaimsValidator extends PostAuthorityClaimsValidator {

	private final CommonParts parts;
	public GetAuthorityClaimsValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(parts::assertOrgDomainRoleClaims)
				.setOptional()
				.build());

		return environment;
	}
}
