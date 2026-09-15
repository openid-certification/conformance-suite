package net.openid.conformance.condition.client;

import net.openid.conformance.util.MdocUtil;
import org.multipaz.cbor.DataItem;
import org.multipaz.mdoc.mso.MobileSecurityObject;

import java.time.Instant;

/**
 * Base for conditions that check the timestamps in the MSO's ValidityInfo structure against
 * each other, the current time and the document signer certificate in the issuerAuth x5chain,
 * as an mdoc reader does in ISO/IEC 18013-5 section 9.3.1 step 5.
 */
public abstract class AbstractValidateMdocMsoValidityInfo extends AbstractValidateMdocDsCertificate {

	protected MobileSecurityObject parseMso(DataItem issuerSigned) {
		try {
			return MdocUtil.parseMso(issuerSigned);
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		}
	}

	protected static Instant toInstant(kotlin.time.Instant instant) {
		return Instant.ofEpochSecond(instant.getEpochSeconds(), instant.getNanosecondsOfSecond());
	}
}
