package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public abstract class AbstractCreateKSAConsentRequest extends AbstractCondition {

	private static final String SAMPLE_MESSAGE_RESOURCE = "/json/ksa/account-access-consent-request.json";

	@Override
	@PreEnvironment(strings = "client_id", required = "server")
	@PostEnvironment(required = "account_requests_endpoint_request")
	public Environment evaluate(Environment env) {

		String clientId = env.getString("client_id");

		JsonObject message = loadSampleMessage();
		customizeMessageData(message.getAsJsonObject("Data"));

		Instant now = Instant.now();
		long iat = now.getEpochSecond();

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", clientId);
		claims.addProperty("iat", iat);
		claims.addProperty("nbf", iat);
		claims.addProperty("exp", now.plus(1, ChronoUnit.HOURS).getEpochSecond());
		String aud = env.getString("server", "issuer");
		if (aud == null || aud.isEmpty()) {
			throw error("The OP issuer is not available to set as the consent request 'aud'; the server discovery document must be fetched first");
		}
		JsonArray audArray = new JsonArray(1)
		audArray.add(aud)
		claims.add("aud", audArray);
		claims.add("message", message);

		env.putObject("account_requests_endpoint_request", claims);
		logSuccess(args("account_requests_endpoint_request", claims));
		return env;
	}

	/** Override to mutate the message Data block (e.g. set ExpirationDateTime). */
	protected void customizeMessageData(JsonObject data) {
		// default: no change
	}

	private JsonObject loadSampleMessage() {
		try (InputStream is = getClass().getResourceAsStream(SAMPLE_MESSAGE_RESOURCE)) {
			if (is == null) {
				throw error("Could not find KSA consent sample resource", args("resource", SAMPLE_MESSAGE_RESOURCE));
			}
			String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			return JsonParser.parseString(json).getAsJsonObject();
		} catch (IOException e) {
			throw error("Failed to read KSA consent sample resource", e);
		}
	}
}
