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

public class CreateBrazilAccountsEndpointResponse extends AbstractOpenBankingApiResponse {

	@Override
	@PreEnvironment(strings = "fapi_interaction_id")
	@PostEnvironment(required = {"accounts_endpoint_response", "accounts_endpoint_response_headers"})
	public Environment evaluate(Environment env) {
		String requestDateTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));

		//Copied from https://openbanking-brasil.github.io/areadesenvolvedor/#lista-de-contas
		JsonObject response = JsonParser.parseString(
			"{\n" +
				"  \"data\": [\n" +
				"    {\n" +
				"      \"brandName\": \"Organização A\",\n" +
				"      \"companyCnpj\": \"21128159000166\",\n" +
				"      \"type\": \"CONTA_DEPOSITO_A_VISTA\",\n" +
				"      \"compeCode\": \"001\",\n" +
				"      \"branchCode\": \"6272\",\n" +
				"      \"number\": \"94088392\",\n" +
				"      \"checkDigit\": \"4\",\n" +
				"      \"accountId\": \"92792126019929279212650822221989319252576\"\n" +
				"    }\n" +
				"  ],\n" +
				"  \"links\": {\n" +
				"    \"self\": \"https://api.banco.com.br/open-banking/api/v1/resource\",\n" +
				"    \"first\": \"https://api.banco.com.br/open-banking/api/v1/resource\",\n" +
				"    \"prev\": \"https://api.banco.com.br/open-banking/api/v1/resource\",\n" +
				"    \"next\": \"https://api.banco.com.br/open-banking/api/v1/resource\",\n" +
				"    \"last\": \"https://api.banco.com.br/open-banking/api/v1/resource\"\n" +
				"  },\n" +
				"  \"meta\": {\n" +
				"    \"totalRecords\": 1,\n" +
				"    \"totalPages\": 1,\n" +
				"    \"requestDateTime\": \"" + requestDateTime + "\"\n" +
				"  }\n" +
				"}").getAsJsonObject();

		String fapiInteractionId = env.getString("fapi_interaction_id");
		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			throw error("Couldn't find FAPI Interaction ID");
		}

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", fapiInteractionId);
		headers.addProperty("content-type", "application/json");

		logSuccess("Created Brazil accounts response " +
				"(Please note that this is a hardcoded response copied from API documentation)",
			args("accounts_endpoint_response", response, "accounts_endpoint_response_headers", headers));

		env.putObject("accounts_endpoint_response", response);
		env.putObject("accounts_endpoint_response_headers", headers);

		return env;

	}

}
