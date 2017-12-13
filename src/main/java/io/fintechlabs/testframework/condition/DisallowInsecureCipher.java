/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.condition;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class DisallowInsecureCipher extends AbstractCondition {

	private static final Collection<String> SECURE_CIPHERS = ImmutableList.of(
		"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
		"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
		"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
		"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
	);

	/**
	 * @param testId
	 * @param log
	 */
	public DisallowInsecureCipher(String testId, EventLog log, boolean optional) {
		super(testId, log, optional, "FAPI-2-8.5-1");
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {

		String tlsTestHost = env.getString("config", "tls.testHost");
		Integer tlsTestPort = env.getInteger("config", "tls.testPort");

		if (Strings.isNullOrEmpty(tlsTestHost)) {
			return error("Couldn't find host to connect for TLS");
		}

		if (tlsTestPort == null) {
			return error("Couldn't find port to connect for TLS");
		}

		// even though we make a TLS1.2 connection we ignore the server cert validation here
		TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}
			}
		};

		try {

			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());

			SSLSocket socket = (SSLSocket) sc.getSocketFactory().createSocket(tlsTestHost, tlsTestPort);
			// set the connection to use only TLS 1.2
			socket.setEnabledProtocols(new String[] {"TLSv1.2"});

			// filter the list of supported ciphers to contain only insecure ciphers

			ArrayList<String> ciphers = Lists.newArrayList(socket.getEnabledCipherSuites());
			ciphers.removeAll(SECURE_CIPHERS);

			String[] insecureCiphers = Iterables.toArray(ciphers, String.class);
			socket.setEnabledCipherSuites(insecureCiphers);

			log("Trying to connect with an insecure cipher (this is not exhaustive: check the " +
				"server configuration manually to verify conformance)",
				args("insecure_ciphers", insecureCiphers));

			// this makes the actual connection
			socket.startHandshake();

			// if we get here, the connection was established with an insecure cipher

			String cipherSuite = socket.getSession().getCipherSuite();

			socket.close();

			return error("Connected with insecure cipher", args("cipher_suite", cipherSuite));
		} catch (GeneralSecurityException e) {
			return error("Failed to configure TLS 1.2 socket", e);
		} catch (IOException e) {
			logSuccess("Connection with insecure cipher was refused (or failed)");
			return env;
		}
	}

}
