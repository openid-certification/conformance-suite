package net.openid.conformance.raidiam.validators.referencesAuthorisationDomainRole;

import com.google.gson.JsonElement;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api url: ****
 * Api endpoint: GET /references/authorisationdomainroles
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory GET Authorisation Domain Role")
public class GetAuthorisationDomainRoleValidator extends PostAuthorisationDomainRoleValidator {

	private final CommonParts parts;
	public GetAuthorisationDomainRoleValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body,
			new ObjectArrayField
				.Builder("content")
				.setValidator(this::assertAuthorisationDomainRole)
				.setOptional()
				.build());
		parts.assertDefaultResponseFields(body);
		return environment;
	}
}
