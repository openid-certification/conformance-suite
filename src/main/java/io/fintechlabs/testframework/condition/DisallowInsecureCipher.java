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
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.crypto.tls.AlertDescription;
import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.tls.CertificateRequest;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.crypto.tls.DefaultTlsClient;
import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.bouncycastle.crypto.tls.TlsAuthentication;
import org.bouncycastle.crypto.tls.TlsClient;
import org.bouncycastle.crypto.tls.TlsClientProtocol;
import org.bouncycastle.crypto.tls.TlsCredentials;
import org.bouncycastle.crypto.tls.TlsFatalAlert;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class DisallowInsecureCipher extends AbstractCondition {

	private static final List<Integer> SECURE_CIPHERS = ImmutableList.of(
		CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
		CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
		CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
		CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
	);

	/**
	 * @param testId
	 * @param log
	 */
	public DisallowInsecureCipher(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String tlsTestHost = env.getString("config", "tls.testHost");
		Integer tlsTestPort = env.getInteger("config", "tls.testPort");

		if (Strings.isNullOrEmpty(tlsTestHost)) {
			return error("Couldn't find host to connect for TLS");
		}

		if (tlsTestPort == null) {
			return error("Couldn't find port to connect for TLS");
		}

		try {
			Socket socket = new Socket(InetAddress.getByName(tlsTestHost), tlsTestPort);

			try {

				TlsClientProtocol protocol = new TlsClientProtocol(socket.getInputStream(), socket.getOutputStream(), new SecureRandom());

				TlsClient client = new DefaultTlsClient() {

					@Override
					public TlsAuthentication getAuthentication() {
						return new TlsAuthentication() {

							@Override
							public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException {
								return null;
							}

							@Override
							public void notifyServerCertificate(Certificate serverCertificate) throws IOException {
								// Don't care
							}
						};
					}

					@Override
					public int[] getCipherSuites() {

						// filter the list of supported ciphers to contain only insecure ciphers
						ArrayList<Integer> ciphers = Lists.newArrayList(Ints.asList(super.getCipherSuites()));
						ciphers.removeAll(SECURE_CIPHERS);
						return Ints.toArray(ciphers);
					}

					@Override
					public ProtocolVersion getMinimumVersion() {
						return ProtocolVersion.TLSv12;
					}

					@Override
					public void notifySelectedCipherSuite(int selectedCipherSuite) {
						error("Server accepted an insecure cipher", args("cipher_suite", selectedCipherSuite));
					}
				};

				log("Trying to connect with an insecure cipher (this is not exhaustive: check the server configuration manually to verify conformance)");

				protocol.connect(client);

				// By the time handshake completes an error should have been thrown, but just in case:
				return error("Connection completed unexpectedly");

			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					// Don't care
				}
			}
		} catch (TlsFatalAlert e) {
			if (e.getCause() instanceof ConditionError) {
				// It's our own error; pass it on
				throw (ConditionError) e.getCause();
			} else if (e.getAlertDescription() == AlertDescription.handshake_failure) {
				logSuccess("Handshake was refused");
				return env;
			} else {
				return error("Failed to make TLS connection", e);
			}
		} catch (IOException e) {
			return error("Failed to make TLS connection", e);
		}
	}

}
