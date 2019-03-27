package io.fintechlabs.testframework.security;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.mitre.jwt.signer.service.JWTSigningAndValidationService;
import org.mitre.jwt.signer.service.impl.JWKSetCacheService;
import org.mitre.jwt.signer.service.impl.SymmetricKeyJWTValidatorCacheService;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.OIDCAuthenticationFilter;
import org.mitre.openid.connect.client.service.AuthRequestOptionsService;
import org.mitre.openid.connect.client.service.ClientConfigurationService;
import org.mitre.openid.connect.client.service.ServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticAuthRequestOptionsService;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.mitre.openid.connect.model.PendingOIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.mitre.oauth2.model.ClientDetailsEntity.AuthMethod.*;

/**
 * Code almost completely copied from OIDCAuthenticationFilter to workaround
 * the issuer mismatch problem when using a Microsoft account.
 *
 * Warning:
 * Contains private fields with the same names as private fields in the super
 * class to keep code changes minimal. See setters for those fields.
 */
public class MSCompatibleOIDCAuthenticationFilter extends OIDCAuthenticationFilter {
	private ServerConfigurationService serverConfigurationService;
	private ClientConfigurationService clientConfigurationService;
	private AuthRequestOptionsService authOptions = new StaticAuthRequestOptionsService();

	private int timeSkewAllowance = 300;

	@Autowired(required = false)
	private HttpClient httpClient;

	// creates JWT signer/validators for symmetric keys
	@Autowired(required = false)
	private SymmetricKeyJWTValidatorCacheService symmetricCacheService;

	// signer based on keypair for this client (for outgoing auth requests)
	@Autowired(required = false)
	private JWTSigningAndValidationService authenticationSignerService;

	@Autowired(required = false)
	private JWKSetCacheService validationServices;

	/**
	 * issuer check will be skipped for these clients
	 * needed for Microsoft support
	 */
	private Set<String> clientsWithNoIssuerCheck;


	/**
	 * Copied from super class
	 * @param session
	 * @param key
	 * @return
	 */
	private static String getStoredSessionString(HttpSession session, String key) {
		Object o = session.getAttribute(key);
		if (o != null && o instanceof String) {
			return o.toString();
		} else {
			return null;
		}
	}

	/**
	 * Copied from super class
	 */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (validationServices == null) {
			validationServices = new JWKSetCacheService();
		}

		if (symmetricCacheService == null) {
			symmetricCacheService = new SymmetricKeyJWTValidatorCacheService();
		}
	}

	/**
	 * Copied from OIDCAuthenticationFilter
	 * Skips issuer check if client id is in clientsWithNoIssuerCheck
	 * @param request
	 * @param response
	 * @return
	 */
	protected Authentication handleAuthorizationCodeResponse(HttpServletRequest request, HttpServletResponse response) {

		String authorizationCode = request.getParameter("code");

		HttpSession session = request.getSession();

		// check for state, if it doesn't match we bail early
		String storedState = getStoredState(session);
		String requestState = request.getParameter("state");
		if (storedState == null || !storedState.equals(requestState)) {
			throw new AuthenticationServiceException("State parameter mismatch on return. Expected " + storedState + " got " + requestState);
		}

		// look up the issuer that we set out to talk to
		String issuer = getStoredSessionString(session, ISSUER_SESSION_VARIABLE);

		// pull the configurations based on that issuer
		ServerConfiguration serverConfig = serverConfigurationService.getServerConfiguration(issuer);
		final RegisteredClient clientConfig = clientConfigurationService.getClientConfiguration(serverConfig);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("code", authorizationCode);
		form.setAll(authOptions.getTokenOptions(serverConfig, clientConfig, request));

		String codeVerifier = getStoredCodeVerifier(session);
		if (codeVerifier != null) {
			form.add("code_verifier", codeVerifier);
		}

		String redirectUri = getStoredSessionString(session, REDIRECT_URI_SESION_VARIABLE);
		if (redirectUri != null) {
			form.add("redirect_uri", redirectUri);
		}

		// Handle Token Endpoint interaction

		if (httpClient == null) {
			httpClient = HttpClientBuilder.create()
				.useSystemProperties()
				.setDefaultRequestConfig(RequestConfig.custom()
					.setSocketTimeout(httpSocketTimeout)
					.build())
				.build();
		}

		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

		RestTemplate restTemplate;

		if (SECRET_BASIC.equals(clientConfig.getTokenEndpointAuthMethod())) {
			// use BASIC auth if configured to do so
			restTemplate = new RestTemplate(factory) {

				@Override
				protected ClientHttpRequest createRequest(URI url, HttpMethod method) throws IOException {
					ClientHttpRequest httpRequest = super.createRequest(url, method);
					httpRequest.getHeaders().add("Authorization",
						String.format("Basic %s", Base64.encode(String.format("%s:%s",
							UriUtils.encodePathSegment(clientConfig.getClientId(), "UTF-8"),
							UriUtils.encodePathSegment(clientConfig.getClientSecret(), "UTF-8")))));

					return httpRequest;
				}
			};
		} else {
			// we're not doing basic auth, figure out what other flavor we have
			restTemplate = new RestTemplate(factory);

			if (SECRET_JWT.equals(clientConfig.getTokenEndpointAuthMethod()) || PRIVATE_KEY.equals(clientConfig.getTokenEndpointAuthMethod())) {
				// do a symmetric secret signed JWT for auth

				JWTSigningAndValidationService signer = null;
				JWSAlgorithm alg = clientConfig.getTokenEndpointAuthSigningAlg();

				if (SECRET_JWT.equals(clientConfig.getTokenEndpointAuthMethod()) &&
					(JWSAlgorithm.HS256.equals(alg)
						|| JWSAlgorithm.HS384.equals(alg)
						|| JWSAlgorithm.HS512.equals(alg))) {

					// generate one based on client secret
					signer = symmetricCacheService.getSymmetricValidtor(clientConfig.getClient());

				} else if (PRIVATE_KEY.equals(clientConfig.getTokenEndpointAuthMethod())) {

					// needs to be wired in to the bean
					signer = authenticationSignerService;

					if (alg == null) {
						alg = authenticationSignerService.getDefaultSigningAlgorithm();
					}
				}

				if (signer == null) {
					throw new AuthenticationServiceException("Couldn't find required signer service for use with private key auth.");
				}

				JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder();

				claimsSet.issuer(clientConfig.getClientId());
				claimsSet.subject(clientConfig.getClientId());
				claimsSet.audience(Lists.newArrayList(serverConfig.getTokenEndpointUri()));
				claimsSet.jwtID(UUID.randomUUID().toString());

				// TODO: make this configurable
				Date exp = new Date(System.currentTimeMillis() + (60 * 1000)); // auth good for 60 seconds
				claimsSet.expirationTime(exp);

				Date now = new Date(System.currentTimeMillis());
				claimsSet.issueTime(now);
				claimsSet.notBeforeTime(now);

				JWSHeader header = new JWSHeader(alg, null, null, null, null, null, null, null, null, null,
					signer.getDefaultSignerKeyId(),
					null, null);
				SignedJWT jwt = new SignedJWT(header, claimsSet.build());

				signer.signJwt(jwt, alg);

				form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
				form.add("client_assertion", jwt.serialize());
			} else {
				//Alternatively use form based auth
				form.add("client_id", clientConfig.getClientId());
				form.add("client_secret", clientConfig.getClientSecret());
			}

		}

		logger.debug("tokenEndpointURI = " + serverConfig.getTokenEndpointUri());
		logger.debug("form = " + form);

		String jsonString = null;

		try {
			jsonString = restTemplate.postForObject(serverConfig.getTokenEndpointUri(), form, String.class);
		} catch (RestClientException e) {
			// Handle error
			logger.error("Token Endpoint error response:  " + e.getMessage());
			throw new AuthenticationServiceException("Unable to obtain Access Token: " + e.getMessage());
		}

		logger.debug("from TokenEndpoint jsonString = " + jsonString);

		JsonElement jsonRoot = new JsonParser().parse(jsonString);
		if (!jsonRoot.isJsonObject()) {
			throw new AuthenticationServiceException("Token Endpoint did not return a JSON object: " + jsonRoot);
		}

		JsonObject tokenResponse = jsonRoot.getAsJsonObject();

		if (tokenResponse.get("error") != null) {

			// Handle error
			String error = tokenResponse.get("error").getAsString();
			logger.error("Token Endpoint returned: " + error);
			throw new AuthenticationServiceException("Unable to obtain Access Token.  Token Endpoint returned: " + error);

		} else {

			// Extract the id_token to insert into the
			// OIDCAuthenticationToken

			// get out all the token strings
			String accessTokenValue = null;
			String idTokenValue = null;
			String refreshTokenValue = null;

			if (tokenResponse.has("access_token")) {
				accessTokenValue = tokenResponse.get("access_token").getAsString();
			} else {
				throw new AuthenticationServiceException("Token Endpoint did not return an access_token: " + jsonString);
			}

			if (tokenResponse.has("id_token")) {
				idTokenValue = tokenResponse.get("id_token").getAsString();
			} else {
				logger.error("Token Endpoint did not return an id_token");
				throw new AuthenticationServiceException("Token Endpoint did not return an id_token");
			}

			if (tokenResponse.has("refresh_token")) {
				refreshTokenValue = tokenResponse.get("refresh_token").getAsString();
			}

			try {
				JWT idToken = JWTParser.parse(idTokenValue);

				// validate our ID Token over a number of tests
				JWTClaimsSet idClaims = idToken.getJWTClaimsSet();

				// check the signature
				JWTSigningAndValidationService jwtValidator = null;

				Algorithm tokenAlg = idToken.getHeader().getAlgorithm();

				Algorithm clientAlg = clientConfig.getIdTokenSignedResponseAlg();

				if (clientAlg != null) {
					if (!clientAlg.equals(tokenAlg)) {
						throw new AuthenticationServiceException("Token algorithm " + tokenAlg + " does not match expected algorithm " + clientAlg);
					}
				}

				if (idToken instanceof PlainJWT) {

					if (clientAlg == null) {
						throw new AuthenticationServiceException("Unsigned ID tokens can only be used if explicitly configured in client.");
					}

					if (tokenAlg != null && !tokenAlg.equals(Algorithm.NONE)) {
						throw new AuthenticationServiceException("Unsigned token received, expected signature with " + tokenAlg);
					}
				} else if (idToken instanceof SignedJWT) {

					SignedJWT signedIdToken = (SignedJWT) idToken;

					if (tokenAlg.equals(JWSAlgorithm.HS256)
						|| tokenAlg.equals(JWSAlgorithm.HS384)
						|| tokenAlg.equals(JWSAlgorithm.HS512)) {

						// generate one based on client secret
						jwtValidator = symmetricCacheService.getSymmetricValidtor(clientConfig.getClient());
					} else {
						// otherwise load from the server's public key
						jwtValidator = validationServices.getValidator(serverConfig.getJwksUri());
					}

					if (jwtValidator != null) {
						if (!jwtValidator.validateSignature(signedIdToken)) {
							throw new AuthenticationServiceException("Signature validation failed");
						}
					} else {
						logger.error("No validation service found. Skipping signature validation");
						throw new AuthenticationServiceException("Unable to find an appropriate signature validator for ID Token.");
					}
				} // TODO: encrypted id tokens

				// check the issuer
				if (idClaims.getIssuer() == null) {
					throw new AuthenticationServiceException("Id Token Issuer is null");
				}
				//code changes for Microsoft compatibility begin
				else if (!idClaims.getIssuer().equals(serverConfig.getIssuer())) {
					if (clientsWithNoIssuerCheck != null && clientsWithNoIssuerCheck.contains(clientConfig.getClientId())) {
						logger.warn("Issuer (" + idClaims.getIssuer() + ") does not match the expected issuer (" + serverConfig.getIssuer() + ") "
							+ "but issuer validation is disabled for this provider");
					} else {
						throw new AuthenticationServiceException("Issuers do not match, expected " + serverConfig.getIssuer() + " got " + idClaims.getIssuer());
					}
				}
				//code changes for Microsoft compatibility end

				// check expiration
				if (idClaims.getExpirationTime() == null) {
					throw new AuthenticationServiceException("Id Token does not have required expiration claim");
				} else {
					// it's not null, see if it's expired
					Date now = new Date(System.currentTimeMillis() - (timeSkewAllowance * 1000));
					if (now.after(idClaims.getExpirationTime())) {
						throw new AuthenticationServiceException("Id Token is expired: " + idClaims.getExpirationTime());
					}
				}

				// check not before
				if (idClaims.getNotBeforeTime() != null) {
					Date now = new Date(System.currentTimeMillis() + (timeSkewAllowance * 1000));
					if (now.before(idClaims.getNotBeforeTime())) {
						throw new AuthenticationServiceException("Id Token not valid untill: " + idClaims.getNotBeforeTime());
					}
				}

				// check issued at
				if (idClaims.getIssueTime() == null) {
					throw new AuthenticationServiceException("Id Token does not have required issued-at claim");
				} else {
					// since it's not null, see if it was issued in the future
					Date now = new Date(System.currentTimeMillis() + (timeSkewAllowance * 1000));
					if (now.before(idClaims.getIssueTime())) {
						throw new AuthenticationServiceException("Id Token was issued in the future: " + idClaims.getIssueTime());
					}
				}

				// check audience
				if (idClaims.getAudience() == null) {
					throw new AuthenticationServiceException("Id token audience is null");
				} else if (!idClaims.getAudience().contains(clientConfig.getClientId())) {
					throw new AuthenticationServiceException("Audience does not match, expected " + clientConfig.getClientId() + " got " + idClaims.getAudience());
				}

				// compare the nonce to our stored claim
				String nonce = idClaims.getStringClaim("nonce");
				if (Strings.isNullOrEmpty(nonce)) {

					logger.error("ID token did not contain a nonce claim.");
					throw new AuthenticationServiceException("ID token did not contain a nonce claim.");
				}

				String storedNonce = getStoredNonce(session);
				if (!nonce.equals(storedNonce)) {
					logger.error("Possible replay attack detected! The comparison of the nonce in the returned "
						+ "ID Token to the session " + NONCE_SESSION_VARIABLE + " failed. Expected " + storedNonce + " got " + nonce + ".");

					throw new AuthenticationServiceException(
						"Possible replay attack detected! The comparison of the nonce in the returned "
							+ "ID Token to the session " + NONCE_SESSION_VARIABLE + " failed. Expected " + storedNonce + " got " + nonce + ".");
				}

				// construct an PendingOIDCAuthenticationToken and return a Authentication object w/the userId and the idToken

				PendingOIDCAuthenticationToken token = new PendingOIDCAuthenticationToken(idClaims.getSubject(), idClaims.getIssuer(),
					serverConfig,
					idToken, accessTokenValue, refreshTokenValue);

				Authentication authentication = this.getAuthenticationManager().authenticate(token);

				return authentication;
			} catch (ParseException e) {
				throw new AuthenticationServiceException("Couldn't parse idToken: ", e);
			}


		}
	}

	@Override
	public ServerConfigurationService getServerConfigurationService() {
		return serverConfigurationService;
	}

	@Override
	public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
		super.setServerConfigurationService(serverConfigurationService);
	}

	@Override
	public ClientConfigurationService getClientConfigurationService() {
		return clientConfigurationService;
	}

	@Override
	public void setClientConfigurationService(ClientConfigurationService clientConfigurationService) {
		this.clientConfigurationService = clientConfigurationService;
		super.setClientConfigurationService(clientConfigurationService);
	}

	public AuthRequestOptionsService getAuthOptions() {
		return authOptions;
	}

	public void setAuthOptions(AuthRequestOptionsService authOptions) {
		this.authOptions = authOptions;
		super.setAuthRequestOptionsService(authOptions);
	}

	@Override
	public int getTimeSkewAllowance() {
		return timeSkewAllowance;
	}

	@Override
	public void setTimeSkewAllowance(int timeSkewAllowance) {
		this.timeSkewAllowance = timeSkewAllowance;
		super.setTimeSkewAllowance(timeSkewAllowance);
	}

	public HttpClient getHttpClient() {
		return httpClient;
	}

	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	public SymmetricKeyJWTValidatorCacheService getSymmetricCacheService() {
		return symmetricCacheService;
	}

	@Override
	public void setSymmetricCacheService(SymmetricKeyJWTValidatorCacheService symmetricCacheService) {
		this.symmetricCacheService = symmetricCacheService;
		super.setSymmetricCacheService(symmetricCacheService);
	}

	public JWTSigningAndValidationService getAuthenticationSignerService() {
		return authenticationSignerService;
	}

	public void setAuthenticationSignerService(JWTSigningAndValidationService authenticationSignerService) {
		this.authenticationSignerService = authenticationSignerService;
	}

	@Override
	public JWKSetCacheService getValidationServices() {
		return validationServices;
	}

	@Override
	public void setValidationServices(JWKSetCacheService validationServices) {
		this.validationServices = validationServices;
		super.setValidationServices(validationServices);
	}

	public Set<String> getClientsWithNoIssuerCheck() {
		return clientsWithNoIssuerCheck;
	}

	public void setClientsWithNoIssuerCheck(Set<String> clientsWithNoIssuerCheck) {
		this.clientsWithNoIssuerCheck = clientsWithNoIssuerCheck;
	}
}
