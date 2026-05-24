package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Per IETF SD-JWT VC draft-13 §6.4: "Consumers MUST retrieve and process
 * Type Metadata for the extended type before processing the Type Metadata
 * for the extending type." And §9.5: "all claim metadata from the extended
 * type MUST be respected and are inherited by the child type."
 *
 * This implementation does not yet fetch and merge the parent Type Metadata
 * chain. Instead it surfaces a WARNING when {@code extends} is present so
 * the test log makes the limitation explicit, while still allowing the
 * downstream mandatory and sd checks to run against the child's directly-
 * declared claims. Per §9.5.1 a child type can only make inherited
 * constraints stricter (sd: allowed→always/never, mandatory: false→true)
 * and MUST NOT relax them — so child-only checks may report false
 * negatives for parent-inherited constraints, but cannot report false
 * positives.
 */
public class VCIDetectTypeMetadataExtends extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		JsonObject typeMetadata = env.getElementFromObject("vci", "sdjwt_vc_type_metadata").getAsJsonObject();
		if (typeMetadata.has("extends")) {
			throw error("Type Metadata document contains 'extends'; the conformance suite does not yet process the parent chain, so inherited mandatory/sd constraints per §9.5 are not verified. The child's directly-declared claim constraints are still checked.",
				args("extends", typeMetadata.get("extends")));
		}
		logSuccess("Type Metadata document has no 'extends'; all declared claim constraints will be checked");
		return env;
	}
}
