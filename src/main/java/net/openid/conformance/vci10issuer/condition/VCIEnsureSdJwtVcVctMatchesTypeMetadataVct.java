package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Per IETF SD-JWT VC draft-13 §6.3: "A consumer retrieving Type Metadata
 * MUST ensure that the vct value in the SD-JWT VC payload is identical to
 * the vct value in the reference to the Type Metadata (either in the SD-JWT
 * VC itself or in an extends property in a Type Metadata document)."
 *
 * This condition handles the non-extends case: it compares {@code sdjwt.credential.claims.vct}
 * against {@code vci.sdjwt_vc_type_metadata.vct}. The extends-chain variant
 * is deferred along with the rest of {@code extends} processing.
 */
public class VCIEnsureSdJwtVcVctMatchesTypeMetadataVct extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "vci"})
	public Environment evaluate(Environment env) {
		String sdJwtVct = env.getString("sdjwt", "credential.claims.vct");
		String typeMetadataVct = env.getString("vci", "sdjwt_vc_type_metadata.vct");

		if (sdJwtVct == null) {
			throw error("SD-JWT VC has no 'vct' claim; cannot verify vct equality per §6.3");
		}
		if (typeMetadataVct == null) {
			throw error("Type Metadata document has no 'vct' property; cannot verify vct equality per §6.3 (and §6.2 requires it)");
		}
		if (!sdJwtVct.equals(typeMetadataVct)) {
			throw error("SD-JWT VC 'vct' claim does not match the Type Metadata document's 'vct' property, violating SD-JWT VC §6.3",
				args("sdjwt_vct", sdJwtVct, "type_metadata_vct", typeMetadataVct));
		}
		logSuccess("SD-JWT VC 'vct' claim matches the Type Metadata document's 'vct' property",
			args("vct", sdJwtVct));
		return env;
	}
}
