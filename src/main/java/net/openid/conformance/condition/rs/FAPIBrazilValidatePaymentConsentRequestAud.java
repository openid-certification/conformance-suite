package net.openid.conformance.condition.rs;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalClientTest;
import net.openid.conformance.runner.TestDispatcher;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPIBrazilValidatePaymentConsentRequestAud extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"new_consent_request"})
	public Environment evaluate(Environment env) {
		JsonElement aud = env.getElementFromObject("new_consent_request", "claims.aud");
		if (aud == null) {
			throw error("Missing audience, consent request does not contain an 'aud' claim");
		}
		String baseUrlMtls = env.getString("base_url").replaceFirst(TestDispatcher.TEST_PATH, TestDispatcher.TEST_MTLS_PATH);
		String url = baseUrlMtls + "/" + AbstractFAPI1AdvancedFinalClientTest.BRAZIL_PAYMENTS_CONSENTS_PATH;
		if (!url.equals(OIDFJSON.getString(aud))) {
			throw error("aud claim value does not match the endpoint url",
						args("expected", url, "actual", aud));
		}
		logSuccess("aud claim matches the endpoint url", args("aud", aud));
		return env;
	}

}
