package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.util.StringUtils;

public class VCIValidateCredentialOfferRequestParams extends AbstractCondition {

	private final JsonObject requestParts;

	public VCIValidateCredentialOfferRequestParams(JsonObject requestParts) {
		this.requestParts = requestParts;
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject queryStringParams = requestParts.get("query_string_params").getAsJsonObject();

		// ensure credential_offer parameter is NOT present
		if (queryStringParams.has("credential_offer") && queryStringParams.has("credential_offer_uri")) {
			throw error("credential_offer and credential_offer_uri cannot be used together", args("query_string_params", queryStringParams));
		}

		if (!queryStringParams.has("credential_offer") && !queryStringParams.has("credential_offer_uri")) {
			throw error("credential_offer or credential_offer_uri parameter is missing", args("query_string_params", queryStringParams));
		}

		if (queryStringParams.has("credential_offer_uri")) {

			String credentialOfferUri = OIDFJSON.getString(queryStringParams.get("credential_offer_uri"));

			if (!StringUtils.hasText(credentialOfferUri)) {
				throw error("Empty credential_offer_uri parameter value found in query parameters", args("query_string_params", requestParts));
			}

			if (!credentialOfferUri.startsWith("https://")) {
				throw error("credential_offer_uri parameter must use https://", args("credential_offer_uri", credentialOfferUri));
			}

			logSuccess("Found credential_offer_uri in query params", args("credential_offer_uri", credentialOfferUri));
			env.putString("vci", "credential_offer_uri", credentialOfferUri);

		} else if (queryStringParams.has("credential_offer")) {

			String credentialOfferJsonString = OIDFJSON.getString(queryStringParams.get("credential_offer"));

			if (!StringUtils.hasText(credentialOfferJsonString)) {
				throw error("Empty credential_offer parameter value found in query parameters", args("query_string_params", requestParts));
			}

			env.putString("vci", "credential_offer_raw", credentialOfferJsonString);

			logSuccess("Found credential offer parameter value", args("credential_offer", credentialOfferJsonString));
		} else {
			throw error("No credential offer found in query parameters", args("query_string_params", requestParts));
		}

		return env;
	}
}
