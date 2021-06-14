package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class ExtractClientMTLSCertificateSubject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "mutual_tls_authentication")
	@PostEnvironment(required = "certificate_subject")
	public Environment evaluate(Environment env) {
		String certString = env.getString("mutual_tls_authentication", "cert");

		if (Strings.isNullOrEmpty(certString)) {
			throw error("Couldn't find TLS client certificate for MTLS");
		}

		CertificateFactory certFactory = null;
		try {
			certFactory = CertificateFactory.getInstance("X.509", "BC");
		} catch (CertificateException | NoSuchProviderException | IllegalArgumentException e) {
			throw error("Couldn't get CertificateFactory", e);
		}

		X509Certificate certificate = generateCertificateFromMTLSCert(certString, certFactory);
		X500Principal x500Principal = certificate.getSubjectX500Principal();

		// we are careful to get the subjectDN in RFC 4514 format here, that is what is required for
		// tls_client_auth_subject_dn as per https://datatracker.ietf.org/doc/html/rfc8705#section-2.1.2
		X500Name x500name = X500Name.getInstance(x500Principal.getEncoded());
		String subjectDn = x500Principal.getName();

		RDN ou = x500name.getRDNs(BCStyle.OU)[0];
		String ouAsString = IETFUtils.valueToString(ou.getFirst().getValue());

		RDN cn = x500name.getRDNs(BCStyle.CN)[0];
		String cnAsString = IETFUtils.valueToString(cn.getFirst().getValue());

		JsonObject o = new JsonObject();
		o.addProperty("subjectdn", subjectDn);
		o.addProperty("ou", ouAsString);
		o.addProperty("cn", cnAsString);

		env.putObject("certificate_subject", o);

		logSuccess("Extracted subject from MTLS certificate", o);

		return env;
	}

	private X509Certificate generateCertificateFromMTLSCert(String certString, CertificateFactory certFactory) {
		byte[] decodedCert;
		try {
			decodedCert = Base64.getDecoder().decode(certString);
		} catch (IllegalArgumentException e) {
			throw error("base64 decode of cert failed", e, args("cert", certString));
		}

		X509Certificate certificate;
		try {
			certificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(decodedCert));
		} catch (CertificateException | IllegalArgumentException e) {
			throw error("Calling generateCertificate on cert failed", e, args("cert", certString));
		}
		return certificate;
	}

}
