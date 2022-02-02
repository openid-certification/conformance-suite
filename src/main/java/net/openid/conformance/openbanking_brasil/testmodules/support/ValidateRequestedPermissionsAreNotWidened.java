package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.StringArrayField;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@ApiName("Ensure Consents Are Not Widened")
public class ValidateRequestedPermissionsAreNotWidened extends AbstractJsonAssertingCondition {
	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		String consentPermissions = environment.getString("consent_permissions");
		Set<String> permissions = Arrays.stream(consentPermissions.split("\\W")).collect(Collectors.toSet());

		JsonElement body = bodyFrom(environment);

		JsonObject data = findByPath(body, ROOT_PATH).getAsJsonObject();

		assertField(data,
			new StringArrayField
				.Builder("permissions")
				.setEnums(permissions)
				.build());

		return environment;
	}

}

