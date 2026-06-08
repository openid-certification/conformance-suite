package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Per IETF SD-JWT VC draft-13 §6.3.1: "If the type is a URL using the HTTPS
 * scheme, Type Metadata MAY be retrieved from it." Reads the {@code vct}
 * claim from the parsed SD-JWT VC and, if it is an HTTPS URL, stores it at
 * {@code vci.sdjwt_vc_type_metadata_url} so the downstream fetch sequence
 * can be gated on its presence.
 *
 * The credential-iteration loop in VCIProfileBehavior reuses the same
 * environment for every issued credential, so this condition first clears
 * any Type-Metadata state left over from the previous iteration to prevent
 * stale fetched documents or readiness flags from being applied to a later
 * credential.
 *
 * Always succeeds; the absence of the stored URL signals to the caller that
 * type-metadata retrieval is not applicable for this credential.
 */
public class VCIExtractSdJwtVcTypeMetadataUrl extends AbstractCondition {

	private static final String[] STALE_KEYS = {
		"sdjwt_vc_type_metadata_url",
		"sdjwt_vc_type_metadata",
		"sdjwt_vc_type_metadata_endpoint_response",
	};

	@Override
	@PreEnvironment(required = "sdjwt")
	public Environment evaluate(Environment env) {
		clearStaleTypeMetadataState(env);

		String vct = env.getString("sdjwt", "credential.claims.vct");
		if (vct == null || vct.isEmpty()) {
			logSuccess("'vct' claim is absent or empty; no type metadata to retrieve");
			return env;
		}
		URI uri;
		try {
			uri = new URI(vct);
		} catch (URISyntaxException e) {
			logSuccess("'vct' is not a syntactically valid URI; no type metadata to retrieve",
				args("vct", vct));
			return env;
		}
		String scheme = uri.getScheme();
		if (!"https".equalsIgnoreCase(scheme)) {
			logSuccess("'vct' is not an HTTPS URL; no type metadata will be retrieved per SD-JWT VC §6.3.1",
				args("vct", vct, "scheme", scheme));
			return env;
		}
		env.putString("vci", "sdjwt_vc_type_metadata_url", vct);
		logSuccess("'vct' is an HTTPS URL; Type Metadata will be retrieved per SD-JWT VC §6.3.1",
			args("vct", vct));
		return env;
	}

	private static void clearStaleTypeMetadataState(Environment env) {
		JsonObject vci = env.getObject("vci");
		if (vci == null) {
			return;
		}
		for (String key : STALE_KEYS) {
			vci.remove(key);
		}
	}
}
