package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Objects;

public class VCIValidateCredentialErrorResponse extends AbstractCondition {

	private final VciErrorCode expectedErrorCode;

	public VCIValidateCredentialErrorResponse(VciErrorCode errorCode) {
		this.expectedErrorCode = Objects.requireNonNull(errorCode);
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject endpointResponse = env.getObject("endpoint_response").getAsJsonObject();
		JsonObject responseBodyJson = JsonParser.parseString(OIDFJSON.getString(endpointResponse.get("body"))).getAsJsonObject();

		if (!responseBodyJson.has("error")) {
			throw error("Required field error is missing in credential error response",
				args("response_body_json", responseBodyJson));
		}

		String error = OIDFJSON.getString(responseBodyJson.get("error"));

		String errorDescription = OIDFJSON.getStringOrNull(responseBodyJson.get("error_description"));

		if (!VciErrorCode.errorCodes().contains(error)) {
			throw error("Non-standard credential error response error code found: " + error,
				args("error", error, "error_description", errorDescription, "expected_error", expectedErrorCode.getErrorCode()));
		}

		if (!expectedErrorCode.getErrorCode().equals(error)) {
			throw error("Expected error code not found in credential error response: " + expectedErrorCode.getErrorCode(),
				args("error", error, "error_description", errorDescription, "expected_error", expectedErrorCode.getErrorCode()));
		}
		logSuccess("Expected error code found in credential error response: " + expectedErrorCode.getErrorCode(),
			args("error", error, "errorDescription", errorDescription, "expected_error", expectedErrorCode.getErrorCode()));

		return env;
	}

}
