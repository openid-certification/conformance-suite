package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.crypto.Algorithm;
import org.multipaz.mdoc.mso.MobileSecurityObject;

import java.util.Base64;

/**
 * HAIP section 8 only requires entities to support SHA-256 for ISO mdoc digests, so a credential
 * whose Mobile Security Object uses another digestAlgorithm (ISO/IEC 18013-5 also allows SHA-384
 * and SHA-512) is valid but may not be accepted by every HAIP verifier. Intended to be called as
 * a warning.
 */
public class WarnIfMdocDigestAlgorithmNotSha256 extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		MobileSecurityObject mso;
		try {
			mso = MdocUtil.parseMso(Base64.getDecoder().decode(env.getString("mdoc_credential_cbor")));
		} catch (MdocUtil.MdocParseException | IllegalArgumentException e) {
			throw error("The mdoc credential could not be parsed, so its digest algorithm cannot be checked: " + e.getMessage(), e);
		}

		Algorithm digestAlgorithm = mso.getDigestAlgorithm();
		if (digestAlgorithm != Algorithm.SHA256) {
			throw error("The digestAlgorithm in the mdoc's Mobile Security Object is not SHA-256. HAIP only requires "
					+ "verifiers to support SHA-256 for mdoc digests, so verifiers may not accept this credential",
				args("digest_algorithm", digestAlgorithm.name()));
		}

		logSuccess("The mdoc's Mobile Security Object uses SHA-256 digests, which all HAIP entities must support",
			args("digest_algorithm", digestAlgorithm.name()));

		return env;
	}
}
