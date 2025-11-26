package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;

public class AustraliaConnectIdEnsureIdTokenContainsVerifiedClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"id_token", "server"} )
	public Environment evaluate(Environment env) {

		// Id token verified claims requested in the request object.
		ArrayList<String> requestedClaims = new ArrayList<>();

		JsonElement authRequestelement = env.getElementFromObject("authorization_endpoint_request", "claims.id_token.verified_claims.claims");

		if (authRequestelement != null) {
			requestedClaims.addAll(authRequestelement.getAsJsonObject().keySet());
		}

		JsonElement element = env.getElementFromObject("id_token", "claims.verified_claims.claims");

		if (element == null) {
			throw error ("id_token does not contain any verified claims", args("expected", requestedClaims));
		}

		// Id token verified claims returned in the id token.
		JsonObject idTokenVerfiedClaims = element.getAsJsonObject();

		ArrayList<String> unexpectedClaims = new ArrayList<>();
		ArrayList<String> missingRequestedClaims = new ArrayList<>(requestedClaims);

		for (String claim: idTokenVerfiedClaims.keySet()) {
			if (missingRequestedClaims.contains(claim)) {
				missingRequestedClaims.remove(claim);
				continue;
			}

			unexpectedClaims.add(claim);
		}

		// Verified claims supported by the server.
		ArrayList<String> supportedClaims = new ArrayList<>();

		element = env.getElementFromObject("server", "claims_in_verified_claims_supported");
		if (element != null && element.isJsonArray()) {
			for (JsonElement claim: element.getAsJsonArray()) {
				supportedClaims.add(OIDFJSON.getString(claim));
			}
		}

		missingRequestedClaims.retainAll(supportedClaims);

		if (! unexpectedClaims.isEmpty() || ! missingRequestedClaims.isEmpty()) {
			throw error ("Unexpected/Missing verified claims returned in id_token",
					args("expected", requestedClaims, "unexpected", unexpectedClaims, "missing", missingRequestedClaims));
		}

		logSuccess("id_token contains only requested verified claims.", args("requested", requestedClaims));

		return env;
	}
}
