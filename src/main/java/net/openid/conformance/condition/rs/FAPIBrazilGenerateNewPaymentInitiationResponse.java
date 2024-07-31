package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class FAPIBrazilGenerateNewPaymentInitiationResponse extends AbstractCondition {
	/*
	{
		"data": [{
			"localInstrument": "DICT",
			"payment": {
				"amount": "100000.12",
				"currency": "BRL"
			},
			"creditorAccount": {
				"ispb": "12345678",
				"issuer": "1774",
				"number": "1234567890",
				"accountType": "CACC"
			},
			"remittanceInformation": "Pagamento da nota XPTO035-002.",
			"qrCode": "00020104141234567890123426660014BR.GOV.BCB.PIX014466756C616E6F32303139406578616D706C652E636F6D27300012  \nBR.COM.OUTRO011001234567895204000053039865406123.455802BR5915NOMEDORECEBEDOR6008BRASILIA61087007490062  \n530515RP12345678-201950300017BR.GOV.BCB.BRCODE01051.0.080450014BR.GOV.BCB.PIX0123PADRAO.URL.PIX/0123AB  \nCD81390012BR.COM.OUTRO01190123.ABCD.3456.WXYZ6304EB76\n",
			"proxy": "12345678901",
			"cnpjInitiator": "61820817000109",
			"endToEndId": "E00000000202407311248Nqa8UwJVdye"
		}]
	}

	COPY data from request as is.
	Then add paymentId, consentId, creationDateTime, statusUpdateDateTime, status, aud, iss, iat, jti claims
	 */
	@Override
	@PreEnvironment(strings = {"fapi_interaction_id", "consent_id"}, required = {"payment_initiation_request"})
	@PostEnvironment(required = {"payment_initiation_response", "payment_initiation_response_headers"})
	public Environment evaluate(Environment env) {

		String consentId = env.getString("consent_id");
		Instant baseDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		String creationDateTime = DateTimeFormatter.ISO_INSTANT.format(baseDate);

		JsonArray requestDataArray = env.getElementFromObject("payment_initiation_request", "claims.data").getAsJsonArray();
		JsonArray responseDataArray = new JsonArray();
		for (JsonElement requestDataElement : requestDataArray) {
			JsonObject requestData = requestDataElement.getAsJsonObject();

			requestData.addProperty("creationDateTime", creationDateTime);
			requestData.addProperty("statusUpdateDateTime", creationDateTime);
			requestData.addProperty("paymentId", UUID.randomUUID().toString());
			requestData.addProperty("consentId", consentId);
			requestData.addProperty("status", "ACSP");

			responseDataArray.add(requestData);
		}

		JsonObject response = new JsonObject();
		response.add("data", responseDataArray);

		JsonObject links = new JsonObject();
		links.addProperty("self", env.getString("base_url") + "/" + FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH);
		response.add("links", links);

		JsonObject meta = new JsonObject();
		meta.addProperty("requestDateTime", creationDateTime);
		response.add("meta", meta);

		response.addProperty("aud", env.getString("client_certificate_subject", "ou"));
		response.addProperty("iat", Instant.now().getEpochSecond());
		response.addProperty("jti", UUID.randomUUID().toString());
		// as this certificate is an organization level one, the uid contains the org it,
		// we store the uid into the now confusingly named 'brazil_software_id'.
		response.addProperty("iss", env.getString("rs_certificate_subject", "brazil_software_id"));

		env.putObject("payment_initiation_response", response);

		String fapiInteractionId = env.getString("fapi_interaction_id");
		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			throw error("Couldn't find FAPI Interaction ID");
		}

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", fapiInteractionId);

		env.putObject("payment_initiation_response_headers", headers);

		logSuccess("Created payment initiation response",
			args("payment_initiation_response", response, "payment_initiation_response_headers", headers));

		return env;

	}

}
