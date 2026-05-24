package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Per IETF SD-JWT VC §6.4: "Consumers MUST retrieve and process Type Metadata
 * for the extended type before processing the Type Metadata for the extending
 * type." And §6.5.2 requires the schema of every type in the chain to be
 * validated, with rejection on any failure.
 *
 * This first cut does not implement chain processing. To avoid producing a
 * partial — and possibly wrong — verdict, if {@code extends} is present we
 * skip the downstream schema validation entirely. The caller gates the schema
 * fetch / validation on the {@code vci.sdjwt_vc_type_metadata_chain_ready}
 * env value that this condition sets only when {@code extends} is absent.
 */
public class VCIDetectTypeMetadataExtends extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		JsonObject typeMetadata = env.getElementFromObject("vci", "sdjwt_vc_type_metadata").getAsJsonObject();
		if (typeMetadata.has("extends")) {
			log("Type Metadata document contains 'extends'; chain-of-types schema validation is not yet implemented in the conformance suite, so downstream schema validation will be skipped to avoid a partial verdict per SD-JWT VC §6.5.2",
				args("extends", typeMetadata.get("extends")));
			return env;
		}
		env.putString("vci", "sdjwt_vc_type_metadata_chain_ready", "true");
		logSuccess("Type Metadata document has no 'extends'; downstream schema validation may proceed");
		return env;
	}
}
