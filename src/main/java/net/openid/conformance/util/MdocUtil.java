package net.openid.conformance.util;

import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.EcPublicKey;
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate;
import org.multipaz.crypto.EcPublicKeyOkp;
import org.multipaz.mdoc.mso.MobileSecurityObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helpers for extracting data from mdoc IssuerSigned structures (ISO 18013-5) as received
 * in OID4VCI credential responses.
 */
public final class MdocUtil {

	private MdocUtil() {
		// utility class
	}

	/**
	 * Thrown when an IssuerSigned structure cannot be parsed or does not contain the expected
	 * data. The message is written to be usable directly as a condition error message.
	 */
	public static class MdocParseException extends Exception {
		private static final long serialVersionUID = 1L;

		public MdocParseException(String message) {
			super(message);
		}

		public MdocParseException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	/**
	 * Parses the Mobile Security Object from the issuerAuth COSE_Sign1 payload of an mdoc
	 * IssuerSigned structure.
	 */
	public static MobileSecurityObject parseMso(byte[] issuerSignedBytes) throws MdocParseException {
		try {
			return parseMso(Cbor.INSTANCE.decode(issuerSignedBytes));
		} catch (MdocParseException e) {
			throw e;
		} catch (Exception e) {
			throw new MdocParseException("Failed to parse the mdoc credential", e);
		}
	}

	/**
	 * Parses the Mobile Security Object from the issuerAuth COSE_Sign1 payload of an already
	 * CBOR-decoded IssuerSigned structure. Use this overload when the caller also needs other
	 * parts of the IssuerSigned structure (e.g. nameSpaces) so the bytes are only decoded once.
	 */
	public static MobileSecurityObject parseMso(DataItem issuerSigned) throws MdocParseException {
		try {
			DataItem issuerAuth = issuerSigned.getOrNull("issuerAuth");
			if (issuerAuth == null) {
				throw new MdocParseException("mdoc credential is missing the required 'issuerAuth' field");
			}
			CoseSign1 coseSign1 = issuerAuth.getAsCoseSign1();
			DataItem msoDataItem = Cbor.INSTANCE.decode(coseSign1.getPayload()).getAsTaggedEncodedCbor();
			return MobileSecurityObject.Companion.fromDataItem(msoDataItem);
		} catch (MdocParseException e) {
			throw e;
		} catch (Exception e) {
			throw new MdocParseException("Failed to parse the MSO from the mdoc credential", e);
		}
	}

	/**
	 * Extracts the X.509 certificate chain from the x5chain header (label 33) of the issuerAuth
	 * COSE_Sign1 of an already CBOR-decoded IssuerSigned structure. The returned chain is
	 * guaranteed non-empty; its first certificate is the document signer certificate.
	 */
	public static org.multipaz.crypto.X509CertChain extractX5chain(DataItem issuerSigned) throws MdocParseException {
		DataItem issuerAuth = issuerSigned.getOrNull("issuerAuth");
		if (issuerAuth == null) {
			throw new MdocParseException("IssuerSigned structure missing 'issuerAuth' field");
		}
		CoseSign1 coseSign1;
		try {
			coseSign1 = issuerAuth.getAsCoseSign1();
		} catch (Exception e) {
			throw new MdocParseException("Failed to parse issuerAuth as COSE_Sign1", e);
		}
		DataItem x5chainItem = coseSign1.getUnprotectedHeaders()
			.get(new org.multipaz.cose.CoseNumberLabel(org.multipaz.cose.Cose.COSE_LABEL_X5CHAIN));
		if (x5chainItem == null) {
			throw new MdocParseException("COSE_Sign1 unprotected headers missing x5chain (label 33)");
		}
		org.multipaz.crypto.X509CertChain certChain;
		try {
			certChain = x5chainItem.getAsX509CertChain();
		} catch (Exception e) {
			throw new MdocParseException("Failed to parse x5chain from COSE_Sign1 unprotected headers", e);
		}
		if (certChain.getCertificates().isEmpty()) {
			throw new MdocParseException("x5chain certificate chain is empty");
		}
		return certChain;
	}

	/**
	 * Extracts the MSO device key (deviceKeyInfo.deviceKey) from an mdoc IssuerSigned
	 * structure and returns it converted to a JWK.
	 */
	public static JsonObject extractDeviceKeyJwk(byte[] issuerSignedBytes) throws MdocParseException {
		return deviceKeyToJwk(parseMso(issuerSignedBytes));
	}

	/**
	 * Converts the device key (deviceKeyInfo.deviceKey) of an already parsed MSO to a JWK.
	 * Both double-coordinate EC keys and the Curve25519/448 (OKP) keys that ISO 18013-5 also
	 * permits are supported.
	 */
	public static JsonObject deviceKeyToJwk(MobileSecurityObject mso) throws MdocParseException {
		EcPublicKey deviceKey = mso.getDeviceKey();

		JsonObject jwk = new JsonObject();
		if (deviceKey instanceof EcPublicKeyDoubleCoordinate ecKey) {
			jwk.addProperty("kty", "EC");
			jwk.addProperty("crv", ecKey.getCurve().getJwkName());
			jwk.addProperty("x", Base64URL.encode(ecKey.getX()).toString());
			jwk.addProperty("y", Base64URL.encode(ecKey.getY()).toString());
		} else if (deviceKey instanceof EcPublicKeyOkp okpKey) {
			jwk.addProperty("kty", "OKP");
			jwk.addProperty("crv", okpKey.getCurve().getJwkName());
			jwk.addProperty("x", Base64URL.encode(okpKey.getX()).toString());
		} else {
			throw new MdocParseException("The mdoc credential's device key is not an EC or OKP key"
				+ " (curve: " + deviceKey.getCurve().name() + ")");
		}
		return jwk;
	}

	/**
	 * Returns the IssuerSignedItemBytes (still in their #6.24 tagged encoded-CBOR form, as
	 * needed for digest calculation) of an mdoc IssuerSigned structure, keyed by namespace.
	 * Returns an empty map when the optional nameSpaces field is absent.
	 */
	public static Map<String, List<DataItem>> getIssuerSignedItems(DataItem issuerSigned) throws MdocParseException {
		DataItem nameSpaces = issuerSigned.getOrNull("nameSpaces");
		if (nameSpaces == null) {
			return Map.of();
		}
		if (!(nameSpaces instanceof CborMap nameSpacesMap)) {
			throw new MdocParseException("The mdoc credential's nameSpaces field is not a CBOR map");
		}
		Map<String, List<DataItem>> itemsByNamespace = new LinkedHashMap<>();
		for (Map.Entry<DataItem, DataItem> namespaceEntry : nameSpacesMap.getItems().entrySet()) {
			try {
				String namespace = namespaceEntry.getKey().getAsTstr();
				itemsByNamespace.put(namespace, ((CborArray) namespaceEntry.getValue()).getItems());
			} catch (Exception e) {
				throw new MdocParseException("Failed to parse the mdoc credential's nameSpaces field", e);
			}
		}
		return itemsByNamespace;
	}
}
