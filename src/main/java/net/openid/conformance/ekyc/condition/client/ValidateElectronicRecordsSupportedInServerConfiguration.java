package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateElectronicRecordsSupportedInServerConfiguration extends AbstractCondition {

	//electronic_records_supported: REQUIRED when evidence_supported contains "electronicrecord".
	// JSON array containing all electronic record types the OP supports (see @!predefinedvalues).
	//Note "electronicrecord" above is wrong, as of version 12 of the spec, see:
	// https://bitbucket.org/openid/ekyc-ida/issues/1264/formatting-issues-in-id3
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonArray evidenceSupportedArray = getJsonArrayFromEnvironment(env, "server", "evidence_supported", "evidence_supported in authorization server metadata");
		JsonElement jsonElement = env.getElementFromObject("server", "electronic_records_supported");
		if(evidenceSupportedArray.contains(new JsonPrimitive("electronic_record"))) {
			if(jsonElement==null) {
				throw error("electronic_records_supported is required when evidence_supported contains electronic_record");
			}
			if(!jsonElement.isJsonArray()) {
				throw error("electronic_records_supported must be an array",
					args("actual", jsonElement));
			}
			// seems a sensible requirement, raised to WG in https://bitbucket.org/openid/ekyc-ida/issues/1265/op-metadata-should-indicate-if-empty
			if(jsonElement.getAsJsonArray().size()<1){
				throw error("electronic_records_supported must have at least one entry");
			}
			for (JsonElement el: jsonElement.getAsJsonArray()) {
				if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
					throw error("The entries in evidence_supported must be JSON strings.", args("actual", jsonElement));
				}
			}
			logSuccess("electronic_records_supported is valid", args("electronic_records_supported", jsonElement));
			return env;
		} else {
			if(jsonElement == null) {
				log("Server configuration does not contain an electronic_records_supported entry");
				return env;
			}
			if(!jsonElement.isJsonArray()) {
				throw error("electronic_records_supported must be a json array", args("actual", jsonElement));
			}
			for (JsonElement el: jsonElement.getAsJsonArray()) {
				if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
					throw error("The entries in evidence_supported must be JSON strings.", args("actual", jsonElement));
				}
			}
			logSuccess("electronic_records_supported is valid");
			return env;
		}
	}

}
