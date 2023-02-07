package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateBrazilResourcesEndpointResponse extends AbstractOpenBankingApiResponse {

	@Override
	@PreEnvironment(strings = "fapi_interaction_id")
	@PostEnvironment(required = {"resources_endpoint_response", "resources_endpoint_response_headers"})
	public Environment evaluate(Environment env) {
		
		//Copied from https://br-openinsurance.github.io/areadesenvolvedor/#api-customers-v100-obtem-os-registros-de-identificacao-da-pessoa-natural
		JsonObject response = JsonParser.parseString(
			"{\n" +
					"  \"data\": [\n" +
					"    {\n" +
					"      \"updateDateTime\": \"2021-05-21T08:30:00Z\",\n" +
					"      \"personalId\": \"578-psd-71md6971kjh-2d414\",\n" +
					"      \"brandName\": \"Organização A\",\n" +
					"      \"civilName\": \"Juan Kaique Cláudio Fernandes\",\n" +
					"      \"socialName\": \"string\",\n" +
					"      \"cpfNumber\": \"string\",\n" +
					"      \"companyInfo\": {\n" +
					"        \"cnpjNumber\": \"01773247000563\",\n" +
					"        \"name\": \"Empresa da Organização A\"\n" +
					"      },\n" +
					"      \"documents\": [\n" +
					"        {\n" +
					"          \"type\": \"CNH\",\n" +
					"          \"number\": \"15291908\",\n" +
					"          \"expirationDate\": \"2023-05-21\",\n" +
					"          \"issueLocation\": \"string\"\n" +
					"        }\n" +
					"      ],\n" +
					"      \"hasBrazilianNationality\": false,\n" +
					"      \"otherNationalitiesInfo\": \"CAN\",\n" +
					"      \"otherDocuments\": {\n" +
					"        \"type\": \"SOCIAL SEC\",\n" +
					"        \"number\": \"15291908\",\n" +
					"        \"country\": \"string\",\n" +
					"        \"expirationDate\": \"2023-05-21\"\n" +
					"      },\n" +
					"      \"contact\": {\n" +
					"        \"postalAddresses\": [\n" +
					"          {\n" +
					"            \"address\": \"Av Naburo Ykesaki, 1270\",\n" +
					"            \"additionalInfo\": \"Fundos\",\n" +
					"            \"districtName\": \"Centro\",\n" +
					"            \"townName\": \"Marília\",\n" +
					"            \"countrySubDivision\": \"SP\",\n" +
					"            \"postCode\": \"17500001\",\n" +
					"            \"country\": \"BRA\"\n" +
					"          }\n" +
					"        ],\n" +
					"        \"phones\": [\n" +
					"          {\n" +
					"            \"countryCallingCode\": \"55\",\n" +
					"            \"areaCode\": \"19\",\n" +
					"            \"number\": \"29875132\",\n" +
					"            \"phoneExtension\": \"932\"\n" +
					"          }\n" +
					"        ],\n" +
					"        \"emails\": [\n" +
					"          {\n" +
					"            \"email\": \"nome@br.net\"\n" +
					"          }\n" +
					"        ]\n" +
					"      },\n" +
					"      \"civilStatusCode\": \"SOLTEIRO\",\n" +
					"      \"sex\": \"FEMININO\",\n" +
					"      \"birthDate\": \"2021-05-21\",\n" +
					"      \"filiation\": {\n" +
					"        \"type\": \"PAI\",\n" +
					"        \"civilName\": \"Marcelo Cláudio Fernandes\"\n" +
					"      },\n" +
					"      \"identificationDetails\": {\n" +
					"        \"civilName\": \"Juan Kaique Cláudio Fernandes\",\n" +
					"        \"cpfNumber\": \"string\"\n" +
					"      }\n" +
					"    }\n" +
					"  ],\n" +
					"  \"links\": {\n" +
					"    \"self\": \"https://api.seguro.com.br/open-insurance/customer/v1\",\n" +
					"    \"first\": \"https://api.seguro.com.br/open-insurance/customer/v1\",\n" +
					"    \"prev\": \"https://api.seguro.com.br/open-insurance/customer/v1\",\n" +
					"    \"next\": \"https://api.seguro.com.br/open-insurance/customer/v1\",\n" +
					"    \"last\": \"https://api.seguro.com.br/open-insurance/customer/v1\"\n" +
					"  },\n" +
					"  \"meta\": {\n" +
					"    \"totalRecords\": 1,\n" +
					"    \"totalPages\": 1,\n" +
					"    \"requestDateTime\": \"2021-05-21T08:30:00Z\"\n" +
					"  }\n" +
					"}"
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
