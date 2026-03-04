package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.testmodule.Environment;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class CreateBackchannelEndpointResponse extends AbstractCondition {

	public static final int EXPIRES_IN = 300;

	@Override
	@PreEnvironment(required = { "backchannel_endpoint_http_request", "backchannel_request_object" })
	@PostEnvironment(required = "backchannel_endpoint_response", strings = { "auth_req_id", "auth_req_id_expiration" })
	public Environment evaluate(Environment env) {
		JsonObject backchannelResponse = new JsonObject();

		addAuthReqId(env, backchannelResponse);
		addExpiresIn(env, backchannelResponse);
		addInterval(env, backchannelResponse);

		env.putObject("backchannel_endpoint_response", backchannelResponse);
		logSuccess("Created backchannel response", args("backchannel_endpoint_response", backchannelResponse));

		return env;
	}

	protected void addExpiresIn(Environment env, JsonObject backchannelResponse) {
		Integer expiresInFromConsent = getExpiresInFromConsentExpiration(env);
		Integer requestedExpiry = env.getInteger("requested_expiry");
		int expiresIn = expiresInFromConsent != null ? expiresInFromConsent : (requestedExpiry != null ? requestedExpiry : EXPIRES_IN);
		backchannelResponse.addProperty("expires_in", expiresIn);
		log("Set expires_in", args("expires_in", expiresIn));

		String authReqIdExpiration = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(expiresIn));
		env.putString("auth_req_id_expiration", authReqIdExpiration);
	}

	private Integer getExpiresInFromConsentExpiration(Environment env) {
		String consentExpirationDateTime = env.getString("consent_response", "data.expirationDateTime");
		if (consentExpirationDateTime == null) {
			return null;
		}

		try {
			Instant consentExpiration = Instant.parse(consentExpirationDateTime);
			long seconds = Duration.between(Instant.now(), consentExpiration).toSeconds();
			if (seconds <= 0) {
				return 1;
			}
			if (seconds > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			return (int) seconds;
		} catch (DateTimeParseException e) {
			log("Ignoring invalid consent expirationDateTime while calculating CIBA expires_in", args(
				"expirationDateTime", consentExpirationDateTime,
				"error", e.getMessage()
			));
			return null;
		}
	}

	protected void addAuthReqId(Environment env, JsonObject backchannelResponse) {
		String authReqId = RFC6749AppendixASyntaxUtils.generateVSChar(40, 10, 0);
		env.putString("auth_req_id", authReqId);
		backchannelResponse.addProperty("auth_req_id", authReqId);
		log("Set auth_req_id", args("auth_req_id", authReqId));
	}

	protected void addInterval(Environment env, JsonObject backchannelResponse) {
		Integer interval = env.getInteger("interval");
		if (interval != null) {
			backchannelResponse.addProperty("interval", interval);
		}
		log("Set interval", args("interval", interval));
	}

}
