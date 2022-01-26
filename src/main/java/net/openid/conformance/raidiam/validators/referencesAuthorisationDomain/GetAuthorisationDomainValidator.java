package net.openid.conformance.raidiam.validators.referencesAuthorisationDomain;

import com.google.gson.JsonElement;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * This class corresponds to {@link GetAuthorisationDomainByDomainNameValidator}
 * Api url: ****
 * Api endpoint: GET /references/authorisationdomains
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Get Authorisation Domain")
public class GetAuthorisationDomainValidator extends GetAuthorisationDomainByDomainNameValidator {

	private final CommonParts parts;
	public GetAuthorisationDomainValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertDefaultResponseFields(body);
		assertField(body,
			new ObjectArrayField
				.Builder("content")
				.setValidator(this::assertAuthorisationDomain)
				.setOptional()
				.build());

		return environment;
	}
}
