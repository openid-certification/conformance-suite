package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cose.CoseSign1;
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

		DataItem issuerSignedItem;
		try {
			issuerSignedItem = Cbor.INSTANCE.decode(bytes);
		} catch (Exception e) {
			throw error("Failed to decode IssuerSigned CBOR", e);
		}

		DataItem issuerAuthItem = issuerSignedItem.getOrNull("issuerAuth");
		if (issuerAuthItem == null) {
			throw error("IssuerSigned structure missing 'issuerAuth' field");
		}

		CoseSign1 coseSign1;
		try {
			coseSign1 = issuerAuthItem.getAsCoseSign1();
		} catch (Exception e) {
			throw error("Failed to parse issuerAuth as COSE_Sign1", e);
		}

		MobileSecurityObject mso;
		try {
			byte[] payload = coseSign1.getPayload();
			DataItem payloadItem = Cbor.INSTANCE.decode(payload);
			DataItem msoDataItem = payloadItem.getAsTaggedEncodedCbor();
			mso = MobileSecurityObject.Companion.fromDataItem(msoDataItem);
		} catch (Exception e) {
			throw error("Failed to parse MobileSecurityObject from issuerAuth payload", e);
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
