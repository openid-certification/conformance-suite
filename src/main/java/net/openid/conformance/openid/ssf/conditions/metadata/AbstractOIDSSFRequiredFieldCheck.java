package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractOIDSSFRequiredFieldCheck extends AbstractCondition {

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement transmitterMetadataEl = env.getElementFromObject("ssf", "transmitter_metadata");
		JsonObject transmitterMetadata = transmitterMetadataEl.getAsJsonObject();

		String requiredField = getRequiredFieldName();
		if (!transmitterMetadata.has(requiredField)) {
			throw error("Missing required field " + requiredField);
		}

		logSuccess("Found required field " + requiredField, args(requiredField, transmitterMetadata.get(requiredField)));

		return env;
	}

	protected abstract String getRequiredFieldName();

}
