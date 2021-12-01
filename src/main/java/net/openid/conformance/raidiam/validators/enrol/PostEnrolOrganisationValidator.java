package net.openid.conformance.raidiam.validators.enrol;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

/**
 * Api url: ****
 * Api endpoint: POST /organisations/{OrganisationId}/enrol
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory POST Enrol Organisation")
public class PostEnrolOrganisationValidator extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertEnrol)
				.setOptional()
				.build());

		return environment;
	}

	private void assertEnrol(JsonObject body) {
		assertField(body, CommonFields.getOrganisationId());

		assertField(body,
			new StringField
				.Builder("ClientSecret")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("RedirectUris")
				.setMaxLength(255)
				.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("TokenEndpointAuthMethod")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("GrantTypes")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(body,
			new StringArrayField
				.Builder("ResponseTypes")
				.setMaxLength(40)
				.setOptional()
				.build());


		assertField(body,
			new StringField
				.Builder("ClientName")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("ClientUri")
				.setMaxLength(255)
				.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("LogoUri")
				.setMaxLength(255)
				.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("TosUri")
				.setMaxLength(255)
				.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("PolicyUri")
				.setMaxLength(255)
				.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("JwksUri")
				.setMaxLength(255)
				.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

//		assertField(body,
//			new ObjectField
//				.Builder("Jwks")
//				.setNullable()
//				.setOptional()
//				.build());
	}
}
