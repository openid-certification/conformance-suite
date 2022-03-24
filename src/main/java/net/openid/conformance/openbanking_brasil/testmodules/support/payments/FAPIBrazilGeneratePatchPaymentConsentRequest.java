package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;
import net.openid.conformance.util.JsonObjectBuilder;

import java.text.ParseException;
import java.util.Map;
import java.util.Optional;

public class FAPIBrazilGeneratePatchPaymentConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_response_full" )
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {
		log(env.getObject("consent_endpoint_response_full"));
		JsonObject response = env.getObject("consent_endpoint_response_full");
		JsonObject decodedResponse;
		try {
			decodedResponse = JWTUtil.jwtStringToJsonObjectForEnvironment(OIDFJSON.getString(response.get("body")));
		} catch (ParseException exception) {
			throw error("Could not parse the body: ", response.getAsJsonObject());
		}
		log(decodedResponse);
		env.putObject("decoded_consent_endpoint_response_full",decodedResponse);
		JsonObject paymentRequestObject = buildFromNewConfigFields(env);
		log(paymentRequestObject);
		env.putObject("consent_endpoint_request", paymentRequestObject);
		logSuccess(args("consent_endpoint_request", paymentRequestObject));
		return env;
	}

	public JsonObject buildFromNewConfigFields(Environment env) {
		String identification = extractOrDie(env, "decoded_consent_endpoint_response_full", "claims.data.loggedUser.document.identification");
		String rel = extractOrDie(env, "decoded_consent_endpoint_response_full", "claims.data.loggedUser.document.rel");


		JsonObjectBuilder patchConsentRequestObject = new JsonObjectBuilder()
			.addFields("data.revocation.loggedUser.document", Map.of("identification",identification,"rel",rel))
			.addFields("data.revocation.reason", Map.of("code","OTHER","additionalInformation",DictHomologKeys.PROXY_EMAIL_STANDARD_ADDITIONALINFORMATION))
			.addField("data.revocation.revokedBy","USER")
			.addField("data.status","REVOKED");

		return patchConsentRequestObject.build();
	}


	private String extractOrDie(Environment env, final String key, final String path) {
		Optional<String> string = Optional.ofNullable(env.getString(key, path));
		return string.orElseThrow(() -> error(String.format("Unable to find element %s in config at %s", key, path)));
	}
}
