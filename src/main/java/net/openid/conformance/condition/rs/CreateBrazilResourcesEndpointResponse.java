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
			"""
			{
				"data": [
				{
					"updateDateTime": "2021-05-21T08:30:00Z",
					"personalId": "578-psd-71md6971kjh-2d414",
					"brandName": "Organização A",
					"civilName": "Juan Kaique Cláudio Fernandes",
					"socialName": "string",
					"cpfNumber": "string",
					"companyInfo": {
					"cnpjNumber": "01773247000563",
					"name": "Empresa da Organização A"
					},
					"documents": [
					{
						"type": "CNH",
						"number": "15291908",
						"expirationDate": "2023-05-21",
						"issueLocation": "string"
					}
					],
					"hasBrazilianNationality": false,
					"otherNationalitiesInfo": "CAN",
					"otherDocuments": {
					"type": "SOCIAL SEC",
					"number": "15291908",
					"country": "string",
					"expirationDate": "2023-05-21"
					},
					"contact": {
					"postalAddresses": [
						{
						"address": "Av Naburo Ykesaki, 1270",
						"additionalInfo": "Fundos",
						"districtName": "Centro",
						"townName": "Marília",
						"countrySubDivision": "SP",
						"postCode": "17500001",
						"country": "BRA"
						}
					],
					"phones": [
						{
						"countryCallingCode": "55",
						"areaCode": "19",
						"number": "29875132",
						"phoneExtension": "932"
						}
					],
					"emails": [
						{
						"email": "nome@br.net"
						}
					]
					},
					"civilStatusCode": "SOLTEIRO",
					"sex": "FEMININO",
					"birthDate": "2021-05-21",
					"filiation": {
					"type": "PAI",
					"civilName": "Marcelo Cláudio Fernandes"
					},
					"identificationDetails": {
					"civilName": "Juan Kaique Cláudio Fernandes",
					"cpfNumber": "string"
					}
				}
				],
				"links": {
				"self": "https://api.seguro.com.br/open-insurance/customer/v1",
				"first": "https://api.seguro.com.br/open-insurance/customer/v1",
				"prev": "https://api.seguro.com.br/open-insurance/customer/v1",
				"next": "https://api.seguro.com.br/open-insurance/customer/v1",
				"last": "https://api.seguro.com.br/open-insurance/customer/v1"
				},
				"meta": {
				"totalRecords": 1,
				"totalPages": 1,
				"requestDateTime": "2021-05-21T08:30:00Z"
				}
			}\
			"""
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
