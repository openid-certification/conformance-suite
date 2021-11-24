package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class VerifyErrorIfPixPostFailsOnQres extends AbstractErrorFromJwtResponseCondition {

	public static final String DETALHE_PGTO_INVALIDO = "COBRANCA_INVALIDA";

	@Override
	public Environment evaluate(Environment env) {

		JsonObject response = env.getObject("resource_endpoint_response_full");
		validateError(response, DETALHE_PGTO_INVALIDO);

		return env;
	}


}
