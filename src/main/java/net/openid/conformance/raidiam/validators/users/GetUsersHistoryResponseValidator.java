package net.openid.conformance.raidiam.validators.users;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: ****
 * Api endpoint: /users/{UserEmailId}/history
 * Api git hash: ****
 *
 */
public class GetUsersHistoryResponseValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public GetUsersHistoryResponseValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

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
