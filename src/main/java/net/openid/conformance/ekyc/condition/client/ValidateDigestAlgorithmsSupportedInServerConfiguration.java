package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateDigestAlgorithmsSupportedInServerConfiguration extends AbstractCondition {

	//digest_algorithms_supported: REQUIRED when OP supports external attachments.
	// JSON array containing all supported digest algorithms which can be used as alg property within
	// the digest object of external attachments. If the OP supports external attachments,
	// at least the algorithm sha-256 MUST be supported by the OP as well.
	// The list of possible digest/hash algorithm names is maintained by IANA in [hash_name_registry]
	// (established by [RFC6920]).
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement algsSupported = env.getElementFromObject("server", "digest_algorithms_supported");
		JsonElement attachmentsSupportedElement = env.getElementFromObject("server", "attachments_supported");
		JsonElement external = new JsonPrimitive("external");
		if(attachmentsSupportedElement!=null
			&& attachmentsSupportedElement.isJsonArray()
			&& attachmentsSupportedElement.getAsJsonArray().contains(external)){
			//mandatory
			if(algsSupported == null) {
				throw error("digest_algorithms_supported is required when attachments_supported contains external");
			}
			if(!algsSupported.isJsonArray()) {
				throw error("digest_algorithms_supported must be a json array", args("actual", algsSupported));
			}
			JsonArray algsSupportedArray = algsSupported.getAsJsonArray();
			if(!algsSupportedArray.contains(new JsonPrimitive("sha-256"))){
				throw error("digest_algorithms_supported must contain sha-256", args("actual", algsSupported));
			}
			logSuccess("digest_algorithms_supported is valid", args("digest_algorithms_supported", algsSupported));
			return env;
		} else {
			//optional
			if(algsSupported == null) {
				log("digest_algorithms_supported is not set");
				return env;
			}
			if(!algsSupported.isJsonArray()) {
				throw error("digest_algorithms_supported must be a json array", args("actual", algsSupported));
			}
			//TODO futher validate entries against IANA registry?
			logSuccess("digest_algorithms_supported is valid", args("actual", algsSupported));
			return env;
		}
	}
}
