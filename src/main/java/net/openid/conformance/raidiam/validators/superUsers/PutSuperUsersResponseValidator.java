package net.openid.conformance.raidiam.validators.superUsers;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.testmodule.Environment;

/**
 * Api url: ****
 * Api endpoint: /users/super/{UserEmailId}
 * Api git hash: ****
 */
@ApiName("Raidiam Directory Put Super Users")
public class PutSuperUsersResponseValidator extends AbstractJsonAssertingCondition {
	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body, CommonFields.getUserEmail());
		assertField(body, CommonFields.getStatus());

		return environment;
	}
}
