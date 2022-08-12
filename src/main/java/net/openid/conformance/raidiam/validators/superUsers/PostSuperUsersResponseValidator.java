package net.openid.conformance.raidiam.validators.superUsers;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

/**
 * Api url: ****
 * Api endpoint: /users/super
 * Api git hash: ****
 */
@ApiName("Raidiam Directory Post Super Users")
public class PostSuperUsersResponseValidator extends AbstractJsonAssertingCondition {
	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);

		assertField(body, new StringField
			.Builder("Email")
			.setOptional()
			.build());

		return environment;
	}
}
