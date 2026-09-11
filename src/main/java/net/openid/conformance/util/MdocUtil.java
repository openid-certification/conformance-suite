package net.openid.conformance.util;

import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.EcCurve;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.crypto.EcPublicKey;
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate;
import org.multipaz.crypto.EcPublicKeyOkp;
import org.multipaz.mdoc.mso.MobileSecurityObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helpers for extracting data from mdoc IssuerSigned structures (ISO 18013-5) as received
 * in OID4VCI credential responses.
 */
public final class MdocUtil {

	/**
	 * The curves ISO/IEC 18013-5 permits with each COSE signature algorithm, stated identically
	 * for the issuerAuth signature (9.1.2.4) and the MSO revocation list (12.3.6.3): "ES256 shall
	 * be used with curves P-256 and brainpoolP256r1. ES384 shall be used with curves P-384,
	 * brainpoolP320r1 and brainpoolP384r1. ES512 shall be used with curves P-521 and
	 * brainpoolP512r1. EdDSA shall be used with curves Ed25519 and Ed448."
	 */
	private static final Map<Algorithm, Set<EcCurve>> CURVES_FOR_ALGORITHM = Map.of(
		Algorithm.ES256, Set.of(EcCurve.P256, EcCurve.BRAINPOOLP256R1),
		Algorithm.ES384, Set.of(EcCurve.P384, EcCurve.BRAINPOOLP320R1, EcCurve.BRAINPOOLP384R1),
		Algorithm.ES512, Set.of(EcCurve.P521, EcCurve.BRAINPOOLP512R1),
		Algorithm.EDDSA, Set.of(EcCurve.ED25519, EcCurve.ED448));

	/** The curve names as ISO/IEC 18013-5 writes them. */
	private static final Map<EcCurve, String> ISO_CURVE_NAMES = Map.of(
		EcCurve.P256, "P-256",
		EcCurve.P384, "P-384",
		EcCurve.P521, "P-521",
		EcCurve.BRAINPOOLP256R1, "brainpoolP256r1",
		EcCurve.BRAINPOOLP320R1, "brainpoolP320r1",
		EcCurve.BRAINPOOLP384R1, "brainpoolP384r1",
		EcCurve.BRAINPOOLP512R1, "brainpoolP512r1",
		EcCurve.ED25519, "Ed25519",
		EcCurve.ED448, "Ed448");

	/** The name ISO/IEC 18013-5 uses for the curve, falling back to its SECG name. */
	public static String isoCurveName(EcCurve curve) {
		return ISO_CURVE_NAMES.getOrDefault(curve, curve.getSECGName());
	}

	/**
	 * Returns why a key on {@code curve} may not be used with {@code algorithm} under ISO/IEC
	 * 18013-5's curve pairing rule, or null when the pairing is permitted. An algorithm the rule
	 * does not list at all is reported as such; whether it is permitted is for the caller's
	 * algorithm check.
	 */
	public static String describeCurveMismatch(Algorithm algorithm, EcCurve curve) {
		Set<EcCurve> permitted = CURVES_FOR_ALGORITHM.get(algorithm);
		if (permitted == null) {
			return algorithm.name() + " is not one of the signature algorithms ISO/IEC 18013-5 pairs with a curve";
		}
		if (permitted.contains(curve)) {
			return null;
		}
		return "ISO/IEC 18013-5 says " + algorithm.name() + " shall be used with "
			+ permitted.stream().map(MdocUtil::isoCurveName).sorted().collect(Collectors.joining(", "))
			+ ", but the signing key is on " + isoCurveName(curve);
	}

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
			return MobileSecurityObject.Companion.fromDataItem(parseMsoDataItem(issuerSigned));
		} catch (MdocParseException e) {
			throw e;
		} catch (Exception e) {
			throw new MdocParseException("Failed to parse the MSO from the mdoc credential", e);
		}
	}

	/**
	 * The MSO of an already CBOR-decoded IssuerSigned structure as the raw CBOR data item, for
	 * checks on details of the encoding that {@link #parseMso} hides, such as how the
	 * validityInfo timestamps are represented.
	 */
	public static DataItem parseMsoDataItem(DataItem issuerSigned) throws MdocParseException {
		try {
			DataItem issuerAuth = issuerSigned.getOrNull("issuerAuth");
			if (issuerAuth == null) {
				throw new MdocParseException("mdoc credential is missing the required 'issuerAuth' field");
			}
			CoseSign1 coseSign1 = issuerAuth.getAsCoseSign1();
			return Cbor.INSTANCE.decode(coseSign1.getPayload()).getAsTaggedEncodedCbor();
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
	public static X509CertChain extractX5chain(DataItem issuerSigned) throws MdocParseException {
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
			.get(new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN));
		if (x5chainItem == null) {
			throw new MdocParseException("COSE_Sign1 unprotected headers missing x5chain (label 33)");
		}
		X509CertChain certChain;
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
