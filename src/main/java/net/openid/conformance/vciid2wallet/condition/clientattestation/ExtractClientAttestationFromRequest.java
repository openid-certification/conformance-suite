package net.openid.conformance.vciid2wallet.condition.clientattestation;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class ExtractClientAttestationFromRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	@PostEnvironment(required = {"client_attestation_object", "client_attestation_pop_object"}, strings = {"client_attestation", "client_attestation_pop"})
	public Environment evaluate(Environment env) {

		JsonObject requestHeaders = env.getElementFromObject("token_endpoint_request", "headers").getAsJsonObject();

		JsonElement clientAttestationHeaderEl = requestHeaders.get("oauth-client-attestation");

		if (clientAttestationHeaderEl == null || Strings.isNullOrEmpty(OIDFJSON.getString(clientAttestationHeaderEl))) {
			throw error("Could not find OAuth-Client-Attestation in request headers", args("headers", requestHeaders));
		}

		JsonElement clientAttestationPopHeaderEl = requestHeaders.get("oauth-client-attestation-pop");

		if (clientAttestationPopHeaderEl == null || Strings.isNullOrEmpty(OIDFJSON.getString(clientAttestationPopHeaderEl))) {
			throw error("Could not find OAuth-Client-Attestation-PoP in request headers", args("headers", requestHeaders));
		}

		// TODO handle concatenated serialization format???
		// see: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-05#section-7

		String clientAttestation = OIDFJSON.getString(clientAttestationHeaderEl);
		JsonObject clientAttestationObj = parseJwt("OAuth-Client-Attestation", clientAttestation);
		env.putString("client_attestation", clientAttestation);
		env.putObject("client_attestation_object", clientAttestationObj);

		String clientAttestationPop = OIDFJSON.getString(clientAttestationPopHeaderEl);
		JsonObject clientAttestationPopObj = parseJwt("OAuth-Client-Attestation-PoP", clientAttestationPop);
		env.putString("client_attestation_pop", clientAttestationPop);
		env.putObject("client_attestation_pop_object", clientAttestationPopObj);

		logSuccess("Parsed client attestation with client attestation PoP", args(
			"client_attestation", clientAttestation, "client_assertion_object", clientAttestationObj,
			"client_attestation_pop", clientAttestationPop, "client_assertion_pop_object", clientAttestationPopObj));

		return env;
	}

	protected JsonObject parseJwt(String type, String jwtString) {
		try {
			return JWTUtil.jwtStringToJsonObjectForEnvironment(jwtString);
		} catch (ParseException e) {
			throw error("Couldn't parse jwt from " + type, e, args("jwt", jwtString));
		}
	}
}
