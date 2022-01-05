package net.openid.conformance.raidiam.validators.openIDProvider;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;

/**
 * Api url: ****
 * Api endpoint: POST /token
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory POST Token")
public class PostTokenValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public PostTokenValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		parts.assertAuthority(body);
		return environment;
	}
}