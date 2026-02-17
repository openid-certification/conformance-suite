package net.openid.conformance.vci10wallet;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class VCIErrorResponseUtil {

	public static void setErrorResponse(Environment env, String errorCode, String errorDescription) {
		JsonObject errorBody = new JsonObject();
		errorBody.addProperty("error", errorCode);
		errorBody.addProperty("error_description", errorDescription);

		JsonObject errorResponse = new JsonObject();
		errorResponse.add("body", errorBody);

		env.putObject("vci", "credential_error_response", errorResponse);
	}
}
