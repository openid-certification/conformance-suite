package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalClientTest;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.UUID;

public class FAPIBrazilGenerateNewPaymentInitiationResponse extends AbstractCondition {
/*
COPY data from request, add paymentId, consentId, creationDateTime, statusUpdateDateTime, status
also add aud, iss, iat, jti
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
		links.addProperty("self", env.getString("base_url") + AbstractFAPI1AdvancedFinalClientTest.BRAZIL_PAYMENT_INITIATION_PATH);
		response.add("links", links);

		JsonObject meta = new JsonObject();
		meta.addProperty("totalRecords", 1);
		meta.addProperty("totalPages", 1);
		meta.addProperty("requestDateTime", creationDateTime);
		response.add("meta", meta);

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
