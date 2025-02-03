package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class FAPIBrazilGenerateNewPaymentsConsentResponse extends AbstractCondition {
	/*
	{
	  "data": {
		"consentId": "urn:bancoex:C1DD33123",
		"creationDateTime": "2021-05-21T08:30:00Z",
		"expirationDateTime": "2021-05-21T08:30:00Z",
		"statusUpdateDateTime": "2021-05-21T08:30:00Z",
		"status": "AWAITING_AUTHORISATION",
		"loggedUser": {
		  "document": {
			"identification": "11111111111",
			"rel": "CPF"
		  }
		},
		"businessEntity": {
		  "document": {
			"identification": "11111111111111",
			"rel": "CNPJ"
		  }
		},
		"creditor": {
		  "personType": "PESSOA_NATURAL",
		  "cpfCnpj": "58764789000137",
		  "name": "Marco Antonio de Brito"
		},
		"payment": {
		  "type": "PIX",
		  "schedule": {
			"single": {
			  "date": "2023-08-23"
			}
		  },
		  "date": "2021-01-01",
		  "currency": "BRL",
		  "amount": "100000.12",
		  "ibgeTownCode": "5300108",
		  "details": {
			"localInstrument": "DICT",
			"qrCode": "00020104141234567890123426660014BR.GOV.BCB.PIX014466756C616E6F32303139406578616D706C652E636F6D27300012\nBR.COM.OUTRO011001234567895204000053039865406123.455802BR5915NOMEDORECEBEDOR6008BRASILIA61087007490062\n530515RP12345678-201950300017BR.GOV.BCB.BRCODE01051.0.080450014BR.GOV.BCB.PIX0123PADRAO.URL.PIX/0123AB\nCD81390012BR.COM.OUTRO01190123.ABCD.3456.WXYZ6304EB76\n",
			"proxy": "12345678901",
			"creditorAccount": {
			  "ispb": "12345678",
			  "issuer": "1774",
			  "number": "1234567890",
			  "accountType": "CACC"
			}
		  }
		},
		"debtorAccount": {
		  "ispb": "12345678",
		  "issuer": "1774",
		  "number": "1234567890",
		  "accountType": "CACC"
		}
	  },
	  "links": {
		"self": "https://api.banco.com.br/open-banking/api/v1/resource"
	  },
	  "meta": {
		"requestDateTime": "2021-05-21T08:30:00Z"
	  }
	}
	*/
	@Override
	@PreEnvironment(strings = {"fapi_interaction_id"}, required = {"new_consent_request"})
	@PostEnvironment(strings = {"consent_id"}, required = {"consent_response", "consent_response_headers"})
	public Environment evaluate(Environment env) {

		String consentId = "urn:conformance:oidf:" + RandomStringUtils.secure().nextAlphanumeric(10);

		env.putString("consent_id", consentId);
		JsonObject consentResponse = new JsonObject();

		JsonObject dataElement = new JsonObject();
		Instant baseDateRough = Instant.now();
		Instant baseDate = baseDateRough.minusNanos(baseDateRough.getNano());
		String creationDateTime = DateTimeFormatter.ISO_INSTANT.format(baseDate);
		String expirationDateTime = DateTimeFormatter.ISO_INSTANT.format(baseDate.plus(2, ChronoUnit.HOURS));

		dataElement.addProperty("consentId", consentId);
		dataElement.addProperty("creationDateTime", creationDateTime);
		dataElement.addProperty("expirationDateTime", expirationDateTime);
		dataElement.addProperty("statusUpdateDateTime", creationDateTime);
		dataElement.addProperty("status", "AWAITING_AUTHORISATION");

		JsonElement loggedUser = env.getElementFromObject("new_consent_request", "claims.data.loggedUser");
		dataElement.add("loggedUser", loggedUser);

		JsonElement businessEntity = env.getElementFromObject("new_consent_request", "claims.data.businessEntity");
		if (businessEntity != null && !businessEntity.isJsonNull()) {
			dataElement.add("businessEntity", businessEntity);
		}

		JsonElement creditor = env.getElementFromObject("new_consent_request", "claims.data.creditor");
		dataElement.add("creditor", creditor);

		JsonElement payment = env.getElementFromObject("new_consent_request", "claims.data.payment");
		dataElement.add("payment", payment);

		JsonElement debtorAccount = env.getElementFromObject("new_consent_request", "claims.data.debtorAccount");
		if (debtorAccount != null) {
			dataElement.add("debtorAccount", debtorAccount);
		}

		consentResponse.add("data", dataElement);

		JsonObject links = new JsonObject();
		links.addProperty("self", env.getString("base_url") + "/" + FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH);
		consentResponse.add("links", links);

		JsonObject meta = new JsonObject();
		meta.addProperty("requestDateTime", creationDateTime);
		consentResponse.add("meta", meta);

		consentResponse.addProperty("aud", env.getString("client_certificate_subject", "ou"));
		consentResponse.addProperty("iat", Instant.now().getEpochSecond());
		consentResponse.addProperty("jti", UUID.randomUUID().toString());
		// as this certificate is an organization level one, the uid contains the org it,
		// we store the uid into the now confusingly named 'brazil_software_id'.
		consentResponse.addProperty("iss", env.getString("rs_certificate_subject", "brazil_software_id"));

		env.putObject("consent_response", consentResponse);

		String fapiInteractionId = env.getString("fapi_interaction_id");
		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			throw error("Couldn't find FAPI Interaction ID");
		}

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", fapiInteractionId);

		env.putObject("consent_response_headers", headers);

		logSuccess("Created consent response", args("consentId", consentId, "consent_response", consentResponse, "headers", headers));

		return env;

	}

}
