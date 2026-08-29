package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates the verifier's request object signing certificate against the ISO/IEC 18013-5 mdoc
 * reader authentication certificate profile (Annex B; the profile's use is recommended rather
 * than mandatory, so this condition is expected to be called as a WARNING): the leaf must be
 * within its validity period, use an EC key, assert only digitalSignature key usage, carry the
 * mandatory mdlReaderAuth extended key usage (1.0.18013.5.1.6 - the ISO/IEC 23220-4
 * mdocReaderAuth 1.0.23220.4.1.6 is recommended alongside) and CRL distribution points. A
 * wallet enforcing the profile rejects the verifier's reader authentication without these,
 * even when the certificate chain itself is trusted.
 */
public class ValidateRequestObjectSignerReaderAuthCertificateProfile extends AbstractCondition {

	public static final String MDL_READER_AUTH_EKU_OID = "1.0.18013.5.1.6";
	public static final String MDOC_READER_AUTH_EKU_OID = "1.0.23220.4.1.6";

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {

		List<Base64> x5c;
		try {
			x5c = SignedJWT.parse(env.getString("authorization_request_object", "value"))
				.getHeader().getX509CertChain();
		} catch (ParseException e) {
			throw error("Error parsing request object JWT", e);
		}
		if (x5c == null || x5c.isEmpty()) {
			// a missing x5c is a FAILURE from ValidateRequestObjectSignatureAgainstX5cHeader
			log("Request object JWT does not contain an x5c header; there is no reader authentication certificate to check");
			return env;
		}

		X509Certificate leaf;
		try {
			leaf = (X509Certificate) CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(x5c.get(0).decode()));
		} catch (Exception e) {
			throw error("Failed to parse the request object x5c leaf certificate", e);
		}
		String subject = leaf.getSubjectX500Principal().getName();

		List<String> violations = new ArrayList<>();

		try {
			leaf.checkValidity();
		} catch (Exception e) {
			violations.add("the certificate is outside its validity period");
		}

		if (!(leaf.getPublicKey() instanceof ECPublicKey)) {
			violations.add("the certificate does not contain an elliptic curve public key");
		}

		boolean[] keyUsage = leaf.getKeyUsage();
		if (keyUsage == null) {
			violations.add("the certificate has no key usage extension; the profile requires digitalSignature");
		} else {
			if (!keyUsage[0]) {
				violations.add("the certificate's key usage does not assert digitalSignature");
			}
			for (int i = 1; i < keyUsage.length; i++) {
				if (keyUsage[i]) {
					violations.add("the certificate's key usage asserts bits other than digitalSignature");
					break;
				}
			}
		}

		List<String> eku;
		try {
			eku = leaf.getExtendedKeyUsage();
		} catch (Exception e) {
			eku = null;
			violations.add("the certificate's extended key usage extension could not be parsed");
		}
		if (eku == null || !eku.contains(MDL_READER_AUTH_EKU_OID)) {
			violations.add("the certificate does not contain the mandatory mdlReaderAuth extended key usage ("
				+ MDL_READER_AUTH_EKU_OID + ")");
		}

		// CRLDistributionPoints (OID 2.5.29.31) is mandatory in the profile
		if (leaf.getExtensionValue("2.5.29.31") == null) {
			violations.add("the certificate does not contain the mandatory CRL distribution points extension");
		}

		if (!violations.isEmpty()) {
			JsonArray violationsJson = new JsonArray();
			violations.forEach(violationsJson::add);
			throw error("The verifier's request object signing certificate does not match the ISO/IEC 18013-5 mdoc reader authentication certificate profile; a wallet enforcing the profile will reject the reader authentication: "
					+ String.join("; ", violations),
				args("subject", subject, "violations", violationsJson));
		}

		logSuccess("The verifier's request object signing certificate matches the ISO/IEC 18013-5 mdoc reader authentication certificate profile",
			args("subject", subject,
				"includes_mdoc_reader_auth_eku", eku != null && eku.contains(MDOC_READER_AUTH_EKU_OID)));

		return env;
	}
}
