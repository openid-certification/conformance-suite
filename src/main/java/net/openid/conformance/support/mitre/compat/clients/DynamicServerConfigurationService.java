package net.openid.conformance.support.mitre.compat.clients;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsBoolean;
import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsEncryptionMethodList;
import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsJweAlgorithmList;
import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsJwsAlgorithmList;
import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsString;
import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsStringList;

/**
 *
 * Dynamically fetches OpenID Connect server configurations based on the issuer. Caches the server configurations.
 *
 * @author jricher
 *
 */
public class DynamicServerConfigurationService implements ServerConfigurationService {

	/**
	 * Logger for this class
	 */
	private static final Logger logger = LoggerFactory.getLogger(DynamicServerConfigurationService.class);

	// map of issuer -> server configuration, loaded dynamically from service discovery
	private LoadingCache<String, ServerConfiguration> servers;

	private Set<String> whitelist = new HashSet<>();
	private Set<String> blacklist = new HashSet<>();

	public DynamicServerConfigurationService() {
		this(HttpClientBuilder.create().useSystemProperties().build());
	}

	public DynamicServerConfigurationService(HttpClient httpClient) {
		// initialize the cache
		servers = CacheBuilder.newBuilder().build(new OpenIDConnectServiceConfigurationFetcher(httpClient));
	}

	/**
	 * @return the whitelist
	 */
	public Set<String> getWhitelist() {
		return whitelist;
	}

	/**
	 * @param whitelist the whitelist to set
	 */
	public void setWhitelist(Set<String> whitelist) {
		this.whitelist = whitelist;
	}

	/**
	 * @return the blacklist
	 */
	public Set<String> getBlacklist() {
		return blacklist;
	}

	/**
	 * @param blacklist the blacklist to set
	 */
	public void setBlacklist(Set<String> blacklist) {
		this.blacklist = blacklist;
	}

	@Override
	public ServerConfiguration getServerConfiguration(String issuer) {
		try {

			if (!whitelist.isEmpty() && !whitelist.contains(issuer)) {
				throw new AuthenticationServiceException("Whitelist was nonempty, issuer was not in whitelist: " + issuer);
			}

			if (blacklist.contains(issuer)) {
				throw new AuthenticationServiceException("Issuer was in blacklist: " + issuer);
			}

			return servers.get(issuer);
		} catch (UncheckedExecutionException | ExecutionException e) {
			logger.warn("Couldn't load configuration for " + issuer + ": " + e);
			return null;
		}

	}

	/**
	 * @author jricher
	 *
	 */
	private class OpenIDConnectServiceConfigurationFetcher extends CacheLoader<String, ServerConfiguration> {
		private final HttpComponentsClientHttpRequestFactory httpFactory;

		OpenIDConnectServiceConfigurationFetcher(HttpClient httpClient) {
			this.httpFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		}

		@Override
		public ServerConfiguration load(String issuer) throws Exception {
			RestTemplate restTemplate = new RestTemplate(httpFactory);

			// data holder
			ServerConfiguration conf = new ServerConfiguration();

			// construct the well-known URI
			String url = issuer + "/.well-known/openid-configuration";

			// fetch the value
			String jsonString = restTemplate.getForObject(url, String.class);

			JsonElement parsed = JsonParser.parseString(jsonString);
			if (parsed.isJsonObject()) {

				JsonObject o = parsed.getAsJsonObject();

				// sanity checks
				if (!o.has("issuer")) {
					throw new IllegalStateException("Returned object did not have an 'issuer' field");
				}

				if (!issuer.equals(o.get("issuer").getAsString())) {
					logger.info("Issuer used for discover was " + issuer + " but final issuer is " + o.get("issuer").getAsString());
				}

				conf.setIssuer(o.get("issuer").getAsString());


				conf.setAuthorizationEndpointUri(getAsString(o, "authorization_endpoint"));
				conf.setTokenEndpointUri(getAsString(o, "token_endpoint"));
				conf.setJwksUri(getAsString(o, "jwks_uri"));
				conf.setUserInfoUri(getAsString(o, "userinfo_endpoint"));
				conf.setRegistrationEndpointUri(getAsString(o, "registration_endpoint"));
				conf.setIntrospectionEndpointUri(getAsString(o, "introspection_endpoint"));
				conf.setAcrValuesSupported(getAsStringList(o, "acr_values_supported"));
				conf.setCheckSessionIframe(getAsString(o, "check_session_iframe"));
				conf.setClaimsLocalesSupported(getAsStringList(o, "claims_locales_supported"));
				conf.setClaimsParameterSupported(getAsBoolean(o, "claims_parameter_supported"));
				conf.setClaimsSupported(getAsStringList(o, "claims_supported"));
				conf.setDisplayValuesSupported(getAsStringList(o, "display_values_supported"));
				conf.setEndSessionEndpoint(getAsString(o, "end_session_endpoint"));
				conf.setGrantTypesSupported(getAsStringList(o, "grant_types_supported"));
				conf.setIdTokenSigningAlgValuesSupported(getAsJwsAlgorithmList(o, "id_token_signing_alg_values_supported"));
				conf.setIdTokenEncryptionAlgValuesSupported(getAsJweAlgorithmList(o, "id_token_encryption_alg_values_supported"));
				conf.setIdTokenEncryptionEncValuesSupported(getAsEncryptionMethodList(o, "id_token_encryption_enc_values_supported"));
				conf.setOpPolicyUri(getAsString(o, "op_policy_uri"));
				conf.setOpTosUri(getAsString(o, "op_tos_uri"));
				conf.setRequestObjectEncryptionAlgValuesSupported(getAsJweAlgorithmList(o, "request_object_encryption_alg_values_supported"));
				conf.setRequestObjectEncryptionEncValuesSupported(getAsEncryptionMethodList(o, "request_object_encryption_enc_values_supported"));
				conf.setRequestObjectSigningAlgValuesSupported(getAsJwsAlgorithmList(o, "request_object_signing_alg_values_supported"));
				conf.setRequestParameterSupported(getAsBoolean(o, "request_parameter_supported"));
				conf.setRequestUriParameterSupported(getAsBoolean(o, "request_uri_parameter_supported"));
				conf.setResponseTypesSupported(getAsStringList(o, "response_types_supported"));
				conf.setScopesSupported(getAsStringList(o, "scopes_supported"));
				conf.setSubjectTypesSupported(getAsStringList(o, "subject_types_supported"));
				conf.setServiceDocumentation(getAsString(o, "service_documentation"));
				conf.setTokenEndpointAuthMethodsSupported(getAsStringList(o, "token_endpoint_auth_methods"));
				conf.setTokenEndpointAuthSigningAlgValuesSupported(getAsJwsAlgorithmList(o, "token_endpoint_auth_signing_alg_values_supported"));
				conf.setUiLocalesSupported(getAsStringList(o, "ui_locales_supported"));
				conf.setUserinfoEncryptionAlgValuesSupported(getAsJweAlgorithmList(o, "userinfo_encryption_alg_values_supported"));
				conf.setUserinfoEncryptionEncValuesSupported(getAsEncryptionMethodList(o, "userinfo_encryption_enc_values_supported"));
				conf.setUserinfoSigningAlgValuesSupported(getAsJwsAlgorithmList(o, "userinfo_signing_alg_values_supported"));

				return conf;
			} else {
				throw new IllegalStateException("Couldn't parse server discovery results for " + url);
			}

		}

	}

}
