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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class EnsureTls12 extends AbstractCondition {
	
	/**
	 * @param testId
	 * @param log
	 */
	public EnsureTls12(String testId, EventLog log, boolean optional) {
		super(testId, log, optional, "FAPI-1-7.1-1");
		// TODO Auto-generated constructor stub
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

		    // this makes the actual connection
		    socket.startHandshake();
		    
		    String cipherSuite = socket.getSession().getCipherSuite();
		    
		    
		    
		    socket.close();
		    
		    logSuccess("TLS Connection information", args(
		    		"cipher_suite", cipherSuite
		    		// TODO: log the server certificates from: 
		    		//   socket.getSession().getPeerCertificates(); 
		    		));
		    
		    return env;
		    
		} catch (GeneralSecurityException e) {
			return error("Couldn't configure TLS connection", e);
		} catch (IOException e) {
			return error("Couldn't connect to TLS test URL", e);
		} finally {
			
		}
		
		
	}

}
