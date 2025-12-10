package net.openid.conformance.vci10issuer.util;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;

public class VCICredentialErrorResponseUtil {

	public static void updateCredentialErrorResponseInEnv(Environment env, VciErrorCode errorCode, String errorDescription) {
		JsonObject credentialErrorResponseBody = new JsonObject();
		credentialErrorResponseBody.addProperty("error", errorCode.getErrorCode());
		credentialErrorResponseBody.addProperty("error_description", errorDescription);
		env.putObject("vci", "credential_error_response.body", credentialErrorResponseBody);
	}
}
