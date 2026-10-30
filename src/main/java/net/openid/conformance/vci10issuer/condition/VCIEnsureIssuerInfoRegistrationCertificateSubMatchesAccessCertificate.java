package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.X509CertificateUtil;
import org.bouncycastle.asn1.x500.style.BCStyle;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

/**
 * Checks that the {@code sub} of the registration certificate in {@code issuer_info} equals the
 * organizationIdentifier of the access certificate that signs the issuer metadata, the x5c leaf of the
 * signed metadata, which binds the registration certificate to the issuer.
 */
public class VCIEnsureIssuerInfoRegistrationCertificateSubMatchesAccessCertificate extends AbstractCondition {

	@Override
	@PreEnvironment(required = {VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "credential_issuer_metadata_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonObject payload = env.getElementFromObject(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "payload").getAsJsonObject();
		JsonElement subEl = payload.get("sub");
		if (subEl == null || !subEl.isJsonPrimitive() || !subEl.getAsJsonPrimitive().isString()) {
			throw error("The registration certificate in 'issuer_info' has no 'sub' string", args("payload", payload));
		}
		String sub = OIDFJSON.getString(subEl);

		String signedMetadata = env.getString("credential_issuer_metadata_endpoint_response", "body");
		List<Base64> x5c;
		try {
			x5c = SignedJWT.parse(signedMetadata).getHeader().getX509CertChain();
		} catch (ParseException e) {
			throw error("Unable to parse the signed credential issuer metadata", e, args("signed_metadata", signedMetadata));
		}
		if (x5c == null || x5c.isEmpty()) {
			throw error("The signed credential issuer metadata has no 'x5c' header with the issuer's access certificate, so the"
				+ " registration certificate in 'issuer_info' cannot be bound to the issuer", args("sub", sub));
		}

		List<X509Certificate> certs;
		try {
			certs = X509CertificateUtil.parseX5cCertificatesFromNimbusBase64(x5c);
		} catch (X509CertificateUtil.X5cCertificateChainException e) {
			throw error(e.getMessage());
		}
		X509Certificate accessCertificate = certs.get(0);
		String organizationIdentifier = X509CertificateUtil.subjectRdnValue(accessCertificate, BCStyle.ORGANIZATION_IDENTIFIER);
		if (organizationIdentifier == null) {
			throw error("The certificate signing the issuer metadata has no organizationIdentifier in its subject, so the"
					+ " registration certificate in 'issuer_info' cannot be bound to it",
				args("sub", sub, "access_certificate_subject", accessCertificate.getSubjectX500Principal().getName()));
		}
		if (!organizationIdentifier.equals(sub)) {
			throw error("The 'sub' of the registration certificate in 'issuer_info' is not the organizationIdentifier of the"
					+ " certificate signing the issuer metadata",
				args("sub", sub, "organization_identifier", organizationIdentifier,
					"access_certificate_subject", accessCertificate.getSubjectX500Principal().getName()));
		}

		logSuccess("The 'sub' of the registration certificate in 'issuer_info' is the organizationIdentifier of the certificate"
			+ " signing the issuer metadata", args("sub", sub, "access_certificate_subject", accessCertificate.getSubjectX500Principal().getName()));
		return env;
	}
}
