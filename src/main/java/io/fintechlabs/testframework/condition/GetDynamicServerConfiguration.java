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
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class GetDynamicServerConfiguration extends AbstractCondition {

	private static final Logger logger = LoggerFactory.getLogger(GetDynamicServerConfiguration.class);

	/**
	 * @param testId
	 * @param log
	 */
	public GetDynamicServerConfiguration(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment, java.lang.String, io.fintechlabs.testframework.logging.EventLog)
	 */
	@Override
	public Environment evaluate(Environment env) {
		
		if (!env.containsObj("config")) {
			return error("Couldn't find a configuration");
		}
		
		String staticIssuer = env.getString("config", "server.issuer");
		
		if (!Strings.isNullOrEmpty(staticIssuer)) {
			return error("Static configuration element found, skipping dynamic server discovery", args("issuer", staticIssuer));
		}
		
		String discoveryUrl = env.getString("config", "server.discoveryUrl");
		
		if (Strings.isNullOrEmpty(discoveryUrl)) {

			String iss = env.getString("config", "server.discoveryIssuer");
			discoveryUrl = iss + "/.well-known/openid-configuration";
			
			if (Strings.isNullOrEmpty(iss)) {
				return error("Couldn't find discoveryUrl or discoveryIssuer field for discovery purposes");
			}
			
		}
		
		// get out the server configuration component
		if (!Strings.isNullOrEmpty(discoveryUrl)) {
			// do an auto-discovery here

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

			// fetch the value
			String jsonString;
			try {
				jsonString = restTemplate.getForObject(discoveryUrl, String.class);
			} catch (RestClientResponseException e) {
				return error("Unable to fetch server configuration from " + discoveryUrl, e);
			}

			if (!Strings.isNullOrEmpty(jsonString)) {
				log("Downloaded server configuration", 
						args("server_config_string", jsonString));

				try {
					JsonObject serverConfig = new JsonParser().parse(jsonString).getAsJsonObject();
					
					logSuccess("Successfully parsed server configuration", serverConfig);
					
					env.put("server", serverConfig);
					
					return env;
				} catch (JsonSyntaxException e) {
					return error(e);
				}
				
				
			} else {
				return error("empty server configuration");
			}
			
		} else {
			return error("Couldn't find or construct a discovery URL");
		}

		
		
	}

}
