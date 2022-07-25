package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class EnsureConsentErrorWasDataPgtoInvalida extends AbstractErrorFromJwtResponseCondition  {

	public static final String EXPECTED_ERROR = "DATA_PGTO_INVALIDA";

	@Override
	public Environment evaluate(Environment env) {

		JsonObject response = env.getObject("consent_endpoint_response_full");
		validateError(response, EXPECTED_ERROR);

		return env;
	}

}
