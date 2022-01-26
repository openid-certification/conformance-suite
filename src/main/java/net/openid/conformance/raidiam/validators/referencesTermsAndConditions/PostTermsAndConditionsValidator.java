package net.openid.conformance.raidiam.validators.referencesTermsAndConditions;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
/**
 * Api url: ****
 * Api endpoint: POST /references/termsandconditions
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory POST References - Terms and Conditions")
public class PostTermsAndConditionsValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public PostTermsAndConditionsValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		parts.assertTermsAndConditionsItem(body);
		return environment;
	}
}
