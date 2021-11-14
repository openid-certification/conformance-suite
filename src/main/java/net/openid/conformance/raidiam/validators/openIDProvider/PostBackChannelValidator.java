package net.openid.conformance.raidiam.validators.openIDProvider;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.StringField;

/**
 * Api url: ****
 * Api endpoint: POST /backchannel
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory POST Back Channel")
public class PostBackChannelValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertRequest(body);
		return environment;
	}

	protected void assertRequest(JsonObject body) {
		assertField(body,
			new BooleanField
				.Builder("active")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("client_id")
				.setMaxLength(30)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("exp")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("iat")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("iss")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("jti")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("scope")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("token_type")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("sub")
				.setOptional()
				.build());
	}
}
