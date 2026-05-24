package net.openid.conformance.vci10issuer.condition;

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
 * Always succeeds; the absence of the stored URL signals to the caller that
 * type-metadata retrieval is not applicable for this credential.
 */
public class VCIExtractSdJwtVcTypeMetadataUrl extends AbstractCondition {

	@Override
	@PreEnvironment(required = "sdjwt")
	public Environment evaluate(Environment env) {
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
}
