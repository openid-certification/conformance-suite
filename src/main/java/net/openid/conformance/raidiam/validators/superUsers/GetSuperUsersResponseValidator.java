package net.openid.conformance.raidiam.validators.superUsers;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;

/**
 * Api url: ****
 * Api endpoint: /users/super
 * Api git hash: ****
 */
@ApiName("Raidiam Directory Get Super Users")
public class GetSuperUsersResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(data -> {
					assertField(data, CommonFields.getStatus());
					assertField(data, CommonFields.getUserEmail());

				})
				.build());
		return environment;
	}


}
