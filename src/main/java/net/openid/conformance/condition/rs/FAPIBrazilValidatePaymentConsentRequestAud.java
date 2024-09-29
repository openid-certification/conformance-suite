package net.openid.conformance.condition.rs;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPIBrazilValidatePaymentConsentRequestAud extends AbstractCondition {

	@Override
	@PreEnvironment(strings="base_mtls_url", required = {"new_consent_request"})
	public Environment evaluate(Environment env) {
		String baseUrlMtls = env.getString("base_mtls_url") ;
		String url = baseUrlMtls + "/" + FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH;

		JsonElement aud = env.getElementFromObject("new_consent_request", "claims.aud");
		if (aud == null) {
			throw error("Missing audience, consent request does not contain an 'aud' claim");
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(url))) {
				throw error("aud claim array does not contain the endpoint url", args("expected", url, "actual", aud));
			}
			logSuccess("aud claim contains the endpoint url", args("aud", aud));
		} else {
			if (!url.equals(OIDFJSON.getString(aud))) {
				throw error("aud claim value does not match the endpoint url", args("expected", url, "actual", aud));
			}
			logSuccess("aud claim matches the endpoint url", args("aud", aud));
		}

		return env;
	}

}
