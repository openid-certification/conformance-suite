package net.openid.conformance.raidiam.validators.users;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api url: ****
 * Api endpoint: /users/{UserEmailId}/history
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Post Users History")
public class GetUsersHistoryResponseValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public GetUsersHistoryResponseValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		parts.assertDefaultResponseFields(body);
		assertField(body,
			new ObjectArrayField
				.Builder("content")
				.setValidator(parts::assertTermsAndConditionsDetail)
				.setOptional()
				.build());

		return environment;
	}
}
