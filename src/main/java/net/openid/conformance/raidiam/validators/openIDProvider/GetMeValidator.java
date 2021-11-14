package net.openid.conformance.raidiam.validators.openIDProvider;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.StringField;
/**
 * Api url: ****
 * Api endpoint: GET /me
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory GET Me")
public class GetMeValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertField(body,
			new StringField
				.Builder("sub")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("family_name")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("given_name")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("name")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("email")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("email_verified")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("address")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("phone_number")
				.setOptional()
				.build());

		assertField(body,
			new BooleanField
				.Builder("phone_number_verified")
				.setOptional()
				.build());


		return environment;
	}
}
