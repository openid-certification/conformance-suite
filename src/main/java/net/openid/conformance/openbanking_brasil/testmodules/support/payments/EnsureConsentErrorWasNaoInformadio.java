package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class EnsureConsentErrorWasNaoInformadio extends AbstractErrorFromJwtResponseCondition  {

	public static final String EXPECTED_ERROR = "NAO_INFORMADO";

	@Override
	public Environment evaluate(Environment env) {

		JsonObject response = env.getObject("consent_endpoint_response_full");
		validateError(response, EXPECTED_ERROR);

		return env;
	}

}
