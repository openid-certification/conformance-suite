package net.openid.conformance.raidiam.validators.referencesAuthorityAuthorisationDomain;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api url: ****
 * Api endpoint: GET /references/authoritydomainmapping
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory GET Authority Authorisation Domain")
public class GetAuthorityAuthorisationDomainValidator extends GetAuthorityAuthorisationDomainByDomainIdValidator {

	private final CommonParts parts;
	public GetAuthorityAuthorisationDomainValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		parts.assertDefaultResponseFields(body);
		assertField(body,
			new ObjectArrayField
				.Builder("content")
				.setValidator(this::assertAuthorityAuthorisationDomain)
				.setOptional()
				.build());

		return environment;
	}
}