package io.fintechlabs.testframework.condition.as;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureClientCertificateMatches extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public EnsureClientCertificateMatches(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
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
