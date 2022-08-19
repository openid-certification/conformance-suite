package net.openid.conformance.raidiam.validators.superUsers;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

/**
 * Api url: ****
 * Api endpoint: /users/super
 * Api git hash: ****
 */
@ApiName("Raidiam Directory Get Super Users")
public class GetSuperUsersResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertField(body,
				new ObjectArrayField
						.Builder("$")
						.setValidator(array -> {
							assertField(array, new StringField
								.Builder("Email")
								.setOptional()
								.build());
							assertField(array, CommonFields.getStatus());
						})
						.build());
		return environment;
	}


}
