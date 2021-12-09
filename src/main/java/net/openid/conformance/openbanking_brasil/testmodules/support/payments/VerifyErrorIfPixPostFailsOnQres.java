package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class VerifyErrorIfPixPostFailsOnQres extends AbstractErrorFromJwtResponseCondition {

	public static final String PAGAMENTO_DIVERGENTE_DO_CONSENTIMENTO = "PAGAMENTO_DIVERGENTE_DO_CONSENTIMENTO";

	@Override
	public Environment evaluate(Environment env) {

		JsonObject response = env.getObject("resource_endpoint_response_full");
		validateError(response, PAGAMENTO_DIVERGENTE_DO_CONSENTIMENTO);

		return env;
	}


}
