package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SubresourceIntegrity;

import java.nio.charset.StandardCharsets;

/**
 * Per IETF SD-JWT VC §6.3.1: "If the claim {@code vct#integrity} is present
 * in the SD-JWT VC, its value {@code vct#integrity} MUST be an 'integrity
 * metadata' string as defined in Section 7." And §7 requires the Consumer to
 * verify the retrieved Type Metadata document against that integrity hash per
 * W3C SRI §3.3.5.
 *
 * No-op (logs success) when {@code vct#integrity} is absent; otherwise FAILURE
 * if the hash is malformed or does not match the fetched response body.
 */
public class VCIVerifyTypeMetadataIntegrity extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "vci"})
	public Environment evaluate(Environment env) {
		String integrity = env.getString("sdjwt", "credential.claims.vct#integrity");
		if (integrity == null || integrity.isEmpty()) {
			logSuccess("'vct#integrity' is not present in the SD-JWT VC; no Type Metadata integrity check applies per SD-JWT VC §7");
			return env;
		}

		String body = env.getString("vci", "sdjwt_vc_type_metadata_endpoint_response.body");
		if (body == null) {
			throw error("Cannot verify 'vct#integrity': fetched Type Metadata response body is not available in the environment");
		}
		String url = env.getString("vci", "sdjwt_vc_type_metadata_url");
		byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

		boolean matches;
		try {
			matches = SubresourceIntegrity.verify(bodyBytes, integrity);
		} catch (IllegalArgumentException e) {
			throw error("'vct#integrity' value is malformed per W3C SRI §3.5", e,
				args("vct#integrity", integrity));
		}
		if (!matches) {
			throw error("Fetched Type Metadata document does not match the 'vct#integrity' hash, violating SD-JWT VC §7 (W3C SRI §3.3.4)",
				args("vct#integrity", integrity, "url", url));
		}
		logSuccess("Fetched Type Metadata document matches the 'vct#integrity' hash",
			args("vct#integrity", integrity));
		return env;
	}
}
