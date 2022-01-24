package net.openid.conformance.raidiam.validators.referencesAuthority;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api url: ****
 * Api endpoint: GET /references/authorities
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory GET References - Authority")
public class GetAuthorityValidator extends PostAuthorityValidator {

	private final CommonParts parts;

	public GetAuthorityValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = initBodyArray(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(parts::assertAuthority)
				.setOptional()
				.build());

		return environment;
	}
}
