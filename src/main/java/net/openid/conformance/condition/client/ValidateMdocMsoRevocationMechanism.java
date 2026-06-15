package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.mdoc.mso.MobileSecurityObject;
import org.multipaz.revocation.RevocationStatus;

import java.util.Base64;

/**
 * Validates that if the MSO contains a status field (revocation information),
 * it uses one of the mechanisms defined in ISO/IEC 18013-5: identifier_list or status_list.
 *
 * If the status field is absent, this condition succeeds — inclusion is optional (MAY).
 * If present but using an unrecognized mechanism, this condition fails — implementations
 * MUST use one of the defined mechanisms.
 */
public class ValidateMdocMsoRevocationMechanism extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		String mdocCborBase64 = env.getString("mdoc_credential_cbor");

		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(mdocCborBase64);
		} catch (Exception e) {
			throw error("Failed to decode mdoc_credential_cbor from base64", e);
		}

		MobileSecurityObject mso;
		try {
			mso = MdocUtil.parseMso(bytes);
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		}

		RevocationStatus revocationStatus = mso.getRevocationStatus();
		if (revocationStatus == null) {
			log("MSO does not contain a status field; revocation information is not included");
			return env;
		}

		if (revocationStatus instanceof RevocationStatus.StatusList statusList) {
			logSuccess("MSO contains a valid status_list revocation mechanism",
				args("uri", statusList.getUri(),
					"idx", statusList.getIdx(),
					"has_certificate", statusList.getCertificate() != null));
		} else if (revocationStatus instanceof RevocationStatus.IdentifierList identifierList) {
			logSuccess("MSO contains a valid identifier_list revocation mechanism",
				args("uri", identifierList.getUri(),
					"has_certificate", identifierList.getCertificate() != null));
		} else {
			throw error("MSO status field does not use a revocation mechanism defined in ISO/IEC 18013-5; "
				+ "must be either identifier_list or status_list");
		}

		return env;
	}
}
