package net.openid.conformance.raidiam.validators.organisationAuthorityDomainClaims;

import com.google.gson.JsonElement;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * This class corresponds to {@link PostAuthorityDomainClaimsValidator}
 * Api url: ****
 * Api endpoint: GET /organisations/{OrganisationId}/authoritydomainclaims
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory GET Authority Domain Claims")
public class GetAuthorityDomainClaimsValidator extends PostAuthorityDomainClaimsValidator {

	private final CommonParts parts;
	public GetAuthorityDomainClaimsValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("content")
				.setValidator(this::assertAuthorityDomainClaims)
				.setOptional()
				.build());
		parts.assertDefaultResponseFields(body);

		return environment;
	}
}
