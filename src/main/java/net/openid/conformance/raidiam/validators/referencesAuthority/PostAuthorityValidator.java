package net.openid.conformance.raidiam.validators.referencesAuthority;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringField;

/**
 * Api url: ****
 * Api endpoint: POST /references/authorities
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory POST References - Authority")
public class PostAuthorityValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertAuthority(body);
		return environment;
	}

	protected void assertAuthority(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("AuthorityId")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("AuthorityName")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("AuthorityCode")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("AuthorityUri")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("AuthorityCountry")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(body, CommonFields.getStatus());
	}
}
