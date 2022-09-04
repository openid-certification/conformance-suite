package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class EnsureClientCertificateMatches extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client_certificate", "client"})
	public Environment evaluate(Environment env) {

		String presentedPem = env.getString("client_certificate", "pem");

		if (Strings.isNullOrEmpty(presentedPem)) {
			throw error("Couldn't find client certificate in PEM format");
		}

		String registeredStr = env.getString("client", "certificate");
		if (Strings.isNullOrEmpty(registeredStr)) {
			throw error("Couldn't find registered client certificate");
		}

		// pre-process the registered certificate for X509 parsing
		String registeredPem = registeredStr.replaceAll("\\s+(?!CERTIFICATE-----)", "\n");


		try {

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate presentedCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(presentedPem.getBytes()));
			X509Certificate registeredCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(registeredPem.getBytes()));

			if (presentedCert.equals(registeredCert)) {
				logSuccess("Presented certificate matches registered certificate", args("actual", presentedPem));
				return env;
			} else {
				throw error("Presented certificate does not match registered certificate", args("actual", presentedPem, "expected", registeredPem));
			}

		} catch (CertificateException e) {
			throw error("Error parsing certificate", e, args("presented", presentedPem, "registered", registeredPem));
		}

	}

}
