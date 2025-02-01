package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

public class OIDSSFDefaultSubjectsTransmitterMetadataCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonObject transmitterMetadata = env.getElementFromObject("ssf","transmitter_metadata").getAsJsonObject();

		if (!transmitterMetadata.has("default_subjects")) {
			log("Skipping missing optional default_subjects field");
			return env;
		}

		String defaultSubjects = OIDFJSON.getString(transmitterMetadata.get("default_subjects"));
		Set<String> allowedValues = Set.of("ALL", "NONE");
		if (!allowedValues.contains(defaultSubjects)) {
			throw error("Found invalid values for default_subjects, only " + allowedValues + " are allowed!",
				args("default_subjects", defaultSubjects));
		}

		logSuccess("Found valid default_subjects", args("default_subjects", defaultSubjects));
		return env;
	}
}
