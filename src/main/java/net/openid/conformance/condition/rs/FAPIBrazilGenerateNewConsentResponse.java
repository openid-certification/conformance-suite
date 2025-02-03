package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class FAPIBrazilGenerateNewConsentResponse extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"fapi_interaction_id"}, required = "new_consent_request")
	@PostEnvironment(strings = {"consent_id"}, required = {"consent_response", "consent_response_headers"})
	public Environment evaluate(Environment env) {

		String consentId = "urn:conformance:oidf:" + RandomStringUtils.secure().nextAlphanumeric(10);

		env.putString("consent_id", consentId);
		JsonObject consentResponse = new JsonObject();

		JsonObject dataElement = new JsonObject();
		Instant baseDateRough = Instant.now();
		Instant baseDate = baseDateRough.minusNanos(baseDateRough.getNano());
		String creationDateTime = DateTimeFormatter.ISO_INSTANT.format(baseDate);
		//TODO check plus-minus values or skip completely
		String expirationDateTime = DateTimeFormatter.ISO_INSTANT.format(baseDate.plus(2, ChronoUnit.HOURS));
		String transactionFromDateTime = DateTimeFormatter.ISO_INSTANT.format(baseDate.minus(5, ChronoUnit.MINUTES));
		String transactionToDateTime = DateTimeFormatter.ISO_INSTANT.format(baseDate.plus(2, ChronoUnit.HOURS));

		dataElement.addProperty("consentId", consentId);
		dataElement.addProperty("creationDateTime", creationDateTime);
		dataElement.addProperty("status", "AWAITING_AUTHORISATION");
		dataElement.addProperty("statusUpdateDateTime", creationDateTime);

		JsonArray requestPermissions = (JsonArray) env.getElementFromObject("new_consent_request", "data.permissions");
		JsonArray permissions = new JsonArray();
		// we use slightly different permissions for opin vs openbanking
		List<String> supportedPermissions = List.of(
			"ACCOUNTS_READ",
			"ACCOUNTS_BALANCES_READ",
			"RESOURCES_READ",
			"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ");
		if (requestPermissions != null) {
			for (String permission : supportedPermissions) {
				if (requestPermissions.contains(new JsonPrimitive(permission))) {
					permissions.add(permission);
				}
			}
		}

		dataElement.add("permissions", permissions);
		dataElement.addProperty("expirationDateTime", expirationDateTime);
		dataElement.addProperty("transactionFromDateTime", transactionFromDateTime);
		dataElement.addProperty("transactionToDateTime", transactionToDateTime);

		JsonObject links = new JsonObject();
		links.addProperty("self", env.getString("base_url") + "/" + FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH);
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
