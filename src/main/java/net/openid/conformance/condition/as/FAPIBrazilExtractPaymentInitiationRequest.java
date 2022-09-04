package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class FAPIBrazilExtractPaymentInitiationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"incoming_request","client", "server_encryption_keys"})

	@PostEnvironment(required = {"payment_initiation_request"})
	public Environment evaluate(Environment env) {

		String requestJwtString = env.getString("incoming_request", "body");
		JsonObject parsedRequest = null;
		try {
			JsonObject client = env.getObject("client");
			JsonObject serverEncKeys = env.getObject("server_encryption_keys");
			parsedRequest = JWTUtil.jwtStringToJsonObjectForEnvironment(requestJwtString, client, serverEncKeys);

			if(parsedRequest==null) {
				throw error("Couldn't extract payment initiation request", args("request", requestJwtString));
			}

		} catch (ParseException e) {
			throw error("Couldn't parse payment initiation request", e, args("request", requestJwtString));
		} catch (JOSEException e) {
			throw error("Payment initiation request decryption failed", e, args("request", requestJwtString));
		}

		JsonObject claims = parsedRequest.get("claims").getAsJsonObject();

		if(!claims.has("data")) {
			throw error("Request must contain a 'data' element", args("request_json", claims));
		}

		logSuccess("Parsed payment initiation request", args("payment_initiation_request", claims));
		env.putObject("payment_initiation_request", parsedRequest);

		return env;

	}

}
