package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
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

public abstract class AbstractFAPIBrazilExtractCertificateSubject extends AbstractCondition {

	protected JsonObject extractSubject(String certString, String emptyUidErrorMessage) {
		X509Certificate certificate = generateCertificateFromMTLSCert(certString);
		X500Principal x500Principal = certificate.getSubjectX500Principal();

		// we are careful to get the subjectDN in RFC 4514 format here, that is what is required for
		// tls_client_auth_subject_dn as per https://datatracker.ietf.org/doc/html/rfc8705#section-2.1.2
		// although I believe technically X500Principal always outputs RFC2253, so some newer OIDs are
		// output as numeric encodings instead of names (see unit test).
		X500Name x500name = X500Name.getInstance(x500Principal.getEncoded());
		String subjectDn = x500Principal.getName();

		RDN[] ouArray = x500name.getRDNs(BCStyle.OU);
		String ouAsString;
		JsonObject o = new JsonObject();
		if (ouArray.length == 0) {
			// 2022 style certificate - OU is removed, OI added, example subjectdn:
			// C=BR,ST=SP,L=LONDON,O=Open Banking Brasil,
			// CN=https://web.conformance.directory.openbankingbrasil.org.br,
			// SERIALNUMBER=43142666000197,BusinessCategory=Government Entity,
			// 1.3.6.1.4.1.311.60.2.1.3=UK,
			// organizationIdentifier=OFBBR-74e929d9-33b6-4d85-8ba7-c146c867a817, <---- this is new
			// UID=10120340-3318-4baf-99e2-0b56729c4ab2
			RDN[] oiArray = x500name.getRDNs(BCStyle.ORGANIZATION_IDENTIFIER);
			if (oiArray.length == 0) {
				throw error("Certificate subjectdn contains neither 'organizational unit name' nor 'organization identifier'");
			}
			String oi = IETFUtils.valueToString(oiArray[0].getFirst().getValue());
			String split[] = oi.split("-", 2);
			if (split.length != 2) {
				throw error("'organization identifier' in the certificate does not contain a '-'");
			}
			o.addProperty("org_type", split[0]); // the OFBBR prefix
			ouAsString = split[1];
		} else {
			RDN ou = ouArray[0];
			ouAsString = IETFUtils.valueToString(ou.getFirst().getValue());
		}
		RDN[] uid = x500name.getRDNs(BCStyle.UID);
		String softwareId;
		if (uid.length == 0) {
			throw error(emptyUidErrorMessage,
				args("subjectdn", subjectDn));
		} else {
			// newer Brazilian style certificate as per
			// https://github.com/OpenBanking-Brasil/specs-seguranca/blob/main/open-banking-brasil-certificate-standards-1_ID1-ptbr.md
			softwareId = IETFUtils.valueToString(uid[0].getFirst().getValue());
		}

		o.addProperty("subjectdn", subjectDn);
		o.addProperty("ou", ouAsString); // This isn't always the actual ou from the certificate, but is always the Brazil organization id
		o.addProperty("brazil_software_id", softwareId);
		return o;
	}

	protected X509Certificate generateCertificateFromMTLSCert(String certString) {
		CertificateFactory certFactory = null;
		try {
			certFactory = CertificateFactory.getInstance("X.509", "BC");
		} catch (CertificateException | NoSuchProviderException | IllegalArgumentException e) {
			throw error("Couldn't get CertificateFactory", e);
		}

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
