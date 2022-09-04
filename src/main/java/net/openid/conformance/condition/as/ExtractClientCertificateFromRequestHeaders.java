package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.x509.GeneralName;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

public class ExtractClientCertificateFromRequestHeaders extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	@PostEnvironment(required = "client_certificate")
	public Environment evaluate(Environment env) {

		// Remove any certificate from a previous connection
		env.removeObject("client_certificate");

		String certStr = env.getString("token_endpoint_request", "headers.x-ssl-cert");
		if (certStr == null) {
			throw error("Client certificate not found; likely the non-mtls version of the endpoint was called");
		}
		if (certStr.equals("(null)")) {
			// "(null)" is particular behaviour of apache's request header, as used in our ingress via:
			// "RequestHeader set X-Ssl-Cert "%{SSL_CLIENT_CERT}s"
			throw error("Client certificate not found; the client did not supply a MTLS certification to the endpoint. In some cases this may be because the client is, incorrectly, configured to supply a TLS certificate only if the server explicitly requires a certificate at the TLS level.");
		}

		try {

			// pre-process the cert string for the PEM parser
			String certPem = certStr.replaceAll("\\s+(?!CERTIFICATE-----)", "\n");

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certPem.getBytes()));

			JsonObject certInfo = new JsonObject();
			certInfo.addProperty("cert", certStr);
			certInfo.addProperty("pem", certPem);

			JsonObject subjectInfo = new JsonObject();
			X500Principal subject = cert.getSubjectX500Principal();
			subjectInfo.addProperty("dn", subject.getName());
			certInfo.add("subject", subjectInfo);

			JsonArray sanDnsNames = new JsonArray();
			JsonArray sanUris = new JsonArray();
			JsonArray sanIPs = new JsonArray();
			JsonArray sanEmails = new JsonArray();

			Collection<List<?>> altNames = cert.getSubjectAlternativeNames();
			if (altNames != null) {
				for(List<?> altName : altNames) {
					if(altName.size()< 2) {
						continue;
					}
					String sanValue = String.valueOf(altName.get(1));
					switch((Integer)altName.get(0)) {
						case GeneralName.dNSName:
							sanDnsNames.add(sanValue);
							break;
						case GeneralName.iPAddress:
							sanIPs.add(sanValue);
							break;
						case GeneralName.uniformResourceIdentifier:
							sanUris.add(sanValue);
							break;
						case GeneralName.rfc822Name:
							sanEmails.add(sanValue);
							break;
					}
				}
			}
			certInfo.add("sanDnsNames", sanDnsNames);
			certInfo.add("sanUris", sanUris);
			certInfo.add("sanIPs", sanIPs);
			certInfo.add("sanEmails", sanEmails);

			env.putObject("client_certificate", certInfo);

			logSuccess("Extracted client certificate", args("client_certificate", certInfo));

			return env;

		} catch (CertificateException e) {
			throw error("Error parsing certificate", e, args("cert", certStr));
		}

	}

}
