package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class CreateBrazilResourcesEndpointResponse extends AbstractOpenBankingApiResponse {

	@Override
	@PreEnvironment(strings = "fapi_interaction_id")
	@PostEnvironment(required = {"resources_endpoint_response", "resources_endpoint_response_headers"})
	public Environment evaluate(Environment env) {
		String requestDateTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));

		//Copied from https://br-openinsurance.github.io/areadesenvolvedor/#api-resources-v100-obtem-a-lista-de-recursos-consentidos-pelo-cliente
		JsonObject response = JsonParser.parseString(
			"{\n" +
				"  \"data\": [\n" +
				"    {\n" +
				"      \"resourceId\": \"25cac914-d8ae-6789-b215-650a6215820d\",\n" +
				"      \"type\": \"CAPITALIZATION_TITLES\",\n" +
				"      \"status\": \"AVAILABLE\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"links\": {\n" +
				"    \"self\": \"https://api.seguradora.com.br/open-insurance/api/v1/resource\",\n" +
				"    \"first\": \"https://api.seguradora.com.br/open-insurance/api/v1/resource\",\n" +
				"    \"prev\": \"https://api.seguradora.com.br/open-insurance/api/v1/resource\",\n" +
				"    \"next\": \"https://api.seguradora.com.br/open-insurance/api/v1/resource\",\n" +
				"    \"last\": \"https://api.seguradora.com.br/open-insurance/api/v1/resource\"\n" +
				"  },\n" +
				"  \"meta\": {\n" +
				"    \"totalRecords\": 1,\n" +
				"    \"totalPages\": 1,\n" +
				"    \"requestDateTime\": \"" + requestDateTime + "\"\n" +
				"  }\n" +
				"}\n"
		).getAsJsonObject();

		String fapiInteractionId = env.getString("fapi_interaction_id");
		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			throw error("Couldn't find FAPI Interaction ID");
		}

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", fapiInteractionId);
		headers.addProperty("content-type", "application/json");

		logSuccess("Created Brazil resources response " +
				"(Please note that this is a hardcoded response copied from API documentation)",
			args("resources_endpoint_response", response, "resources_endpoint_response_headers", headers));

		env.putObject("resources_endpoint_response", response);
		env.putObject("resources_endpoint_response_headers", headers);

		return env;

	}

}
