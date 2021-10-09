package net.openid.conformance.openbanking_brasil.resourcesAPI;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.util.field.*;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ArrayField;
import net.openid.conformance.util.field.StringField;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;

import java.util.Set;

/**
 *  * API: https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/gh-pages/swagger/swagger_resources_apis.yaml
 *  * URL: /resources
 *  * Api git hash: 5b108df41040cc17f2b6c501368b046e10128732
 **/
@ApiName("Resources")
public class EmptyResourcesResponseValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> ENUM_TYPE = Sets.newHashSet("ACCOUNT", "CREDIT_CARD_ACCOUNT", "LOAN", "FINANCING", "UNARRANGED_ACCOUNT_OVERDRAFT", "INVOICE_FINANCING");
	public static final Set<String> ENUM_STATUS = Sets.newHashSet("AVAILABLE", "UNAVAILABLE", "TEMPORARILY_UNAVAILABLE", "PENDING_AUTHORISATION");

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body,
			new ArrayField.Builder("data")
				.setMinItems(0)
				.build());

		assertJsonArrays(body, ROOT_PATH, this::assertInnerFields);
		return environment;
	}

	private void assertInnerFields(JsonObject body) {

		assertFieldNotPresent(body,
			new StringField
				.Builder("resourceId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.setMaxLength(100)
				.setMinLength(1)
				.setOptional()
				.build());

		assertFieldNotPresent(body,
			new StringField
				.Builder("type")
				.setEnums(ENUM_TYPE)
				.build());

		assertFieldNotPresent(body,
			new StringField
				.Builder("status")
				.setEnums(ENUM_STATUS)
				.build());

	}

	private void assertFieldNotPresent(JsonObject jsonObject, Field field) {
		if (!JsonHelper.ifExists(jsonObject, field.getPath())) {
			return;
		} else {
			throw error("Field " + field.getPath() + " was unexpectedly found in the response.");
		}
	}
}

