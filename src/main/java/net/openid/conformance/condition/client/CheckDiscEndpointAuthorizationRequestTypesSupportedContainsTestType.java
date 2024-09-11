package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

public class CheckDiscEndpointAuthorizationRequestTypesSupportedContainsTestType extends AbstractValidateJsonArray {

	private static final String environmentVariable = "authorization_details_types_supported";



	private static final String errorMessageNotEnough = "Server does not support every authorization type at RAR Payload";

	@Override
	@PreEnvironment(required = {"server", "rar"})
	public Environment evaluate(Environment env) {

		JsonElement rarContent = env.getElementFromObject("rar", "payload");
		if (!rarContent.isJsonArray()){
			throw error("Rar content is not an array");
		}

		List<String> types = new ArrayList<>();
		for (JsonElement element : rarContent.getAsJsonArray()) {
			types.add(OIDFJSON.getString(element.getAsJsonObject().get("type")));
		}

		return validate(env, environmentVariable, types, types.size(),
			errorMessageNotEnough);

	}
}
