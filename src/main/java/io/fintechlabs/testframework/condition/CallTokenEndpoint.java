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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.net.ssl.KeyManagerFactory;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CallTokenEndpoint extends AbstractCondition {

	
	private static final Logger logger = LoggerFactory.getLogger(CallTokenEndpoint.class);

	
	/**
	 * @param testId
	 * @param log
	 */
	public CallTokenEndpoint(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {

		if (env.getString("server", "token_endpoint") == null) {
			return error("Couldn't find token endpoint");
		}

		if (!env.containsObj("token_endpoint_request_form_parameters")) {
			return error("Couldn't find request form");
		}

		// build up the form
		JsonObject formJson = env.get("token_endpoint_request_form_parameters");
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (String key : formJson.keySet()) {
			form.add(key, formJson.get(key).getAsString());
		}
		
		// extract the headers for use (below)
		final JsonObject headersJson = env.get("token_endpoint_request_headers");
		
		
		HttpClientBuilder builder = HttpClientBuilder.create()
				.useSystemProperties();
		
		// initialize MTLS if it's available
		if (env.containsObj("mutual_tls_authentication")) {
			
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
				
				// TODO: move this to an extractor?
				String clientCert = env.getString("mutual_tls_authentication", "cert");
				String clientKey = env.getString("mutual_tls_authentication", "key");
				
				byte[] certBytes = Base64.getDecoder().decode(clientCert);
				byte[] keyBytes = Base64.getDecoder().decode(clientKey);
				
				X509Certificate cert = generateCertificateFromDER(certBytes);              
			    RSAPrivateKey key  = generatePrivateKeyFromDER(keyBytes);
				
			    KeyStore keystore = KeyStore.getInstance("JKS");
			    keystore.load(null);
			    keystore.setCertificateEntry("cert-alias", cert);
			    keystore.setKeyEntry("key-alias", key, "changeit".toCharArray(), new Certificate[] {cert});

			    /*
				SSLContext sc = new SSLContextBuilder()
						.loadKeyMaterial(keystore, "changeit".toCharArray(), new PrivateKeyStrategy() {
							@Override
							public String chooseAlias(Map<String, PrivateKeyDetails> aliases, Socket socket) {
								return "key-alias";
							}
						})
						.loadTrustMaterial(new TrustStrategy() {
							@Override
							public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
								// trust all server certs
								return true;
							}
						})
						.build();
						*/
			    
			    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			    keyManagerFactory.init(keystore, "changeit".toCharArray());
			    
			    SSLContext sc = SSLContext.getInstance("TLS"); 
			    sc.init(keyManagerFactory.getKeyManagers(), trustAllCerts, new java.security.SecureRandom());
				
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
				
			} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
				logger.warn("TLS Error", e);
				return error("Error when building mutual TLS connection", e);
			} 
			
		}
		
		
		HttpClient httpClient = builder.build();

		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
		
		RestTemplate restTemplate = new RestTemplate(factory){

			@Override
			protected ClientHttpRequest createRequest(URI url, HttpMethod method) throws IOException {
				ClientHttpRequest httpRequest = super.createRequest(url, method);
				if (headersJson != null) {
					// add all the headers
					for (String key : headersJson.keySet()) {
						httpRequest.getHeaders().add(key, headersJson.get(key).getAsString());
					}
				}

				return httpRequest;
			}
		};
		
		String jsonString = null;

		try {
			jsonString = restTemplate.postForObject(env.getString("server", "token_endpoint"), form, String.class);
		} catch (RestClientResponseException e) {

			return error("Error from the token endpoint", e, args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
		}
		
		if (Strings.isNullOrEmpty(jsonString)) {
			return error("Didn't get back a response from the token endpoint");
		} else {
			log("Token endpoint response",
					args("token_endpoint_response", jsonString));
			
			try {
				JsonElement jsonRoot = new JsonParser().parse(jsonString);
				if (jsonRoot == null || !jsonRoot.isJsonObject()) {
					return error("Token Endpoint did not return a JSON object");
				}
	
				logSuccess("Parsed token endpoint response", jsonRoot.getAsJsonObject());
				
				env.put("token_endpoint_response", jsonRoot.getAsJsonObject());
				
				return env;
			} catch (JsonParseException e) {
				return error(e);
			}
		}
		
	}
	
	protected static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
	    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

	    KeyFactory factory = KeyFactory.getInstance("RSA");

	    return (RSAPrivateKey)factory.generatePrivate(spec);        
	}

	protected static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
	    CertificateFactory factory = CertificateFactory.getInstance("X.509");

	    return (X509Certificate)factory.generateCertificate(new ByteArrayInputStream(certBytes));      
	}

}
