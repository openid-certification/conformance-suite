package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalClientTest;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class FAPIBrazilGenerateNewConsentResponse extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"fapi_interaction_id"})
	@PostEnvironment(strings = {"consent_id"}, required = {"consent_response", "consent_response_headers"})
	public Environment evaluate(Environment env) {

		String consentId = "urn:conformance.oidf:" + RandomStringUtils.randomAlphanumeric(10);

		env.putString("consent_id", consentId);
		JsonObject consentResponse = new JsonObject();

		JsonObject dataElement = new JsonObject();
		Instant baseDateRough = Instant.now();
		Instant baseDate = baseDateRough.minusNanos(baseDateRough.getNano());
		String creationDateTime = DateTimeFormatter.ISO_INSTANT.format(baseDate);
		//TODO check plus-minus values or skip completely
		String expirationDateTime = DateTimeFormatter.ISO_INSTANT.format(baseDate.plus(2, ChronoUnit.HOURS));

		dataElement.addProperty("consentId", consentId);
		dataElement.addProperty("creationDateTime", creationDateTime);
		dataElement.addProperty("status", "AWAITING_AUTHORISATION");
		dataElement.addProperty("statusUpdateDateTime", creationDateTime);
		JsonArray permissions = new JsonArray();
		permissions.add("ACCOUNTS_READ");
		permissions.add("ACCOUNTS_BALANCES_READ");
		permissions.add("RESOURCES_READ");

		dataElement.add("permissions", permissions);
		dataElement.addProperty("expirationDateTime", expirationDateTime);

		JsonObject links = new JsonObject();
		links.addProperty("self", env.getString("base_url") + AbstractFAPI1AdvancedFinalClientTest.BRAZIL_CONSENTS_PATH);
		dataElement.add("links", links);

		consentResponse.add("data", dataElement);

		JsonObject meta = new JsonObject();
		meta.addProperty("totalRecords", 1);
		meta.addProperty("totalPages", 1);
		meta.addProperty("requestDateTime", creationDateTime);
		consentResponse.add("meta", meta);

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
