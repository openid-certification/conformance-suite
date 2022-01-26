package net.openid.conformance.raidiam.validators.referencesAuthority;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api url: ****
 * Api endpoint: POST /references/authorities
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory POST References - Authority")
public class PostAuthorityValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public PostAuthorityValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertAuthority(body);
		return environment;
	}
}
