package net.openid.conformance.raidiam.validators.referencesAuthority;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

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
		JsonObject body = bodyFrom(environment);
		parts.assertAuthority(body);
		return environment;
	}
}