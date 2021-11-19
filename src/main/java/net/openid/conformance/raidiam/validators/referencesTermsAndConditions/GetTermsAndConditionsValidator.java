package net.openid.conformance.raidiam.validators.referencesTermsAndConditions;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api url: ****
 * Api endpoint: GET /references/termsandconditions
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory GET References - Terms and Conditions")
public class GetTermsAndConditionsValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	public GetTermsAndConditionsValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		parts.assertDefaultResponseFields(body);
		assertField(body,
			new ObjectArrayField
				.Builder("content")
				.setValidator(parts::assertTermsAndConditionsItem)
				.setOptional()
				.build());

		return environment;
	}
}
