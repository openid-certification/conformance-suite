package net.openid.conformance.raidiam.validators.organisationAuthorityClaimsAuthorisation;

import com.google.gson.JsonElement;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * This class corresponds to {@link PostAuthorityClaimsAuthorisationsValidator}
 * Api endpoint: GET /organisations/{OrganisationId}/authorityclaims/{OrganisationAuthorityClaimId}/authorisations
 */
@ApiName("Raidiam Directory GET Organisation Authority Claims Authorisations")
public class GetAuthorityClaimsAuthorisationsValidator extends PostAuthorityClaimsAuthorisationsValidator {

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("$")
				.setValidator(this::assertAuthorityClaims)
				.setOptional()
				.build());

		return environment;
	}
}
