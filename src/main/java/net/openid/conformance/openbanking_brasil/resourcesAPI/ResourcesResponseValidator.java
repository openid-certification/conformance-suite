package net.openid.conformance.openbanking_brasil.resourcesAPI;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API - Resources "Obtém a lista de recursos consentidos pelo cliente."
 * See <a href="https://openbanking-brasil.github.io/areadesenvolvedor/#obtem-a-lista-de-recursos-consentidos-pelo-cliente">
 *   Obtém a lista de recursos consentidos pelo cliente.</a>
 **/
@ApiName("Resources")
public class ResourcesResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data[0].resourceId");
		assertHasStringField(body, "$.data[0].type");
		assertHasStringField(body, "$.data[0].status");

		return environment;
	}
}
