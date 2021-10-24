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

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertAuthority)
				.setOptional()
				.build());

		return environment;
	}
}
