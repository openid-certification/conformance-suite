package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
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
	  "data": [
		{
		  "paymentId": "TXpRMU9UQTROMWhZV2xSU1FUazJSMDl",
		  "endToEndId": "E9040088820210128000800123873170",
		  "consentId": "urn:bancoex:C1DD33123",
		  "creationDateTime": "2020-07-21T08:30:00Z",
		  "statusUpdateDateTime": "2020-07-21T08:30:00Z",
		  "proxy": "12345678901",
		  "ibgeTownCode": "5300108",
		  "status": "PDNG",
		  "rejectionReason": {
			"code": "SALDO_INSUFICIENTE",
			"detail": "string"
		  },
		  "localInstrument": "DICT",
		  "cnpjInitiator": "50685362000135",
		  "payment": {
			"amount": "100000.12",
			"currency": "BRL"
		  },
		  "transactionIdentification": "E00038166201907261559y6j6",
		  "remittanceInformation": "Pagamento da nota RSTO035-002.",
		  "creditorAccount": {
			"ispb": "12345678",
			"issuer": "1774",
			"number": "1234567890",
			"accountType": "CACC"
		  },
		  "debtorAccount": {
			"ispb": "12345678",
			"issuer": "1774",
			"number": "1234567890",
			"accountType": "CACC"
		  },
		  "authorisationFlow": "HYBRID_FLOW"
		}
	  ],
	  "links": {
		"self": "https://api.banco.com.br/open-banking/api/v1/resource"
	  },
	  "meta": {
		"requestDateTime": "2021-05-21T08:30:00Z"
	  }
	}

	COPY data from request as is.
	Then add paymentId, consentId, creationDateTime, statusUpdateDateTime, status, aud, iss, iat, jti claims
	 */
	@Override
	@PreEnvironment(strings = {"fapi_interaction_id", "consent_id"}, required = {"payment_initiation_request"})
	@PostEnvironment(required = {"payment_initiation_response", "payment_initiation_response_headers"})
	public Environment evaluate(Environment env) {

		String consentId = env.getString("consent_id");
		JsonObject requestData = env.getElementFromObject("payment_initiation_request", "claims.data").getAsJsonObject();
		JsonObject response = new JsonObject();
		requestData.addProperty("paymentId", UUID.randomUUID().toString());
		requestData.addProperty("consentId", consentId);

		Instant baseDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		String creationDateTime = DateTimeFormatter.ISO_INSTANT.format(baseDate);

		requestData.addProperty("creationDateTime", creationDateTime);
		requestData.addProperty("statusUpdateDateTime", creationDateTime);

		requestData.addProperty("status", "ACSP");

		response.add("data", requestData);

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
