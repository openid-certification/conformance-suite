package net.openid.conformance.openbanking_brasil.resourcesAPI.v1;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 *  * API: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_resources_apis.yaml
 *  * URL: /resources
 *  * Api git hash: 5b108df41040cc17f2b6c501368b046e10128732
 **/
@ApiName("Resources")
public class AccountResourcesResponseValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> ENUM_STATUS = Sets.newHashSet("AVAILABLE", "UNAVAILABLE", "TEMPORARILY_UNAVAILABLE", "PENDING_AUTHORISATION");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body,
			new ObjectArrayField.Builder("data")
				.setValidator(this::assertInnerFields)
				.setMinItems(1)
				.build());

		return environment;
	}

	private void assertInnerFields(JsonObject body) {
		assertField(body,
			new StringField
				.Builder("resourceId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.setMaxLength(100)
				.setMinLength(1)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("type")
				.setPattern("ACCOUNT")
				.build());

		assertField(body,
			new StringField
				.Builder("status")
				.setEnums(ENUM_STATUS)
				.build());
	}
}