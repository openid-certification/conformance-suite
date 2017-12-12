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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class FetchServerKeys extends AbstractCondition {

	private static final Logger logger = LoggerFactory.getLogger(FetchServerKeys.class);

	/**
	 * @param testId
	 * @param log
	 */
	public FetchServerKeys(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {
		
		if (!env.containsObj("server")) {
			return error("No server configuration found");
		}
		
		JsonElement jwks = env.findElement("server", "jwks");
		
		if (jwks != null && jwks.isJsonObject()) {
			env.put("server_jwks", jwks.getAsJsonObject());
			logSuccess("Found static server JWKS", args("jwks", jwks));
			return env;
		} else {
			// we don't have a key yet, see if we can fetch it
			
			String jwksUri = env.getString("server", "jwks_uri");
			
			if (!Strings.isNullOrEmpty(jwksUri)) {
				// do the fetch

				HttpClientBuilder builder = HttpClientBuilder.create()
						.useSystemProperties();

				try {
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

					SSLContext sc = SSLContext.getInstance("TLS");
					sc.init(null, trustAllCerts, null); // Use default key managers and secure random
					builder.setSslcontext(sc);

					SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sc,
							new String[]{"TLSv1.2"},
							null,
							SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
					builder.setSSLSocketFactory(sslConnectionSocketFactory);

					Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
							.register("https", sslConnectionSocketFactory)
							.register("http", new PlainConnectionSocketFactory())
							.build();

					HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
					builder.setConnectionManager(ccm);
				} catch (NoSuchAlgorithmException | KeyManagementException e) {
					logger.warn("TLS Error", e);
					return error("Error when building TLS connection", e);
				}

				HttpClient httpClient = builder.build();

				HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

				RestTemplate restTemplate = new RestTemplate(factory);

				log("Fetching server key", args("jwks_uri", jwksUri));
				
				try {
					String jwkString = restTemplate.getForObject(jwksUri, String.class);
					
					log("Found JWK set string", args("jwk_string", jwkString));
					
					// parse the key to make sure it's really a JWK
					JWKSet.parse(jwkString);
					
					// since it parsed, we store it as a JSON object to grab it later on
					JsonObject jwkSet = new JsonParser().parse(jwkString).getAsJsonObject();
					env.put("server_jwks", jwkSet);
					
					logSuccess("Parsed server JWK", args("jwk", jwkSet));
					return env;
					
				} catch (RestClientException e) {
					return error("Exception while fetching server key", e);
				} catch (ParseException e) {
					return error("Unable to parse jwk set", e);
				}
				
			} else {
				return error("Didn't find a JWKS or a JWKS URI in the server configuration");
			}
			
		}
		
		
	}

}
