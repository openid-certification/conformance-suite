package net.openid.conformance.condition.rs;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPIBrazilValidatePaymentInitiationRequestAud extends AbstractCondition {

	@Override
	@PreEnvironment(strings="base_mtls_url", required = {"payment_initiation_request"})
	public Environment evaluate(Environment env) {
		JsonElement aud = env.getElementFromObject("payment_initiation_request", "claims.aud");
		if (aud == null) {
			throw error("Missing audience, payment initiation request does not contain an 'aud' claim");
		}

		String baseUrlMtls = env.getString("base_mtls_url");

		String url = baseUrlMtls + "/" + FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH;
		if (!url.equals(OIDFJSON.getString(aud))) {
			throw error("aud claim value does not match the endpoint url",
						args("expected", url, "actual", aud));
		}
		logSuccess("aud claim matches the endpoint url", args("aud", aud));
		return env;
	}

}
