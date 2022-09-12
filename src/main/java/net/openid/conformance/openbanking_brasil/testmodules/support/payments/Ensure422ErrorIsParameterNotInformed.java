package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class Ensure422ErrorIsParameterNotInformed extends AbstractErrorFromJwtResponseCondition {

	public static final String EXPECTED_ERROR = "PARAMETRO_NAO_INFORMADO";

	@Override
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("resource_endpoint_response_full");
		validateError(response, EXPECTED_ERROR);

		return env;
	}
}
