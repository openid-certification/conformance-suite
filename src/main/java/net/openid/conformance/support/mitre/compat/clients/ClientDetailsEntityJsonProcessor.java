package net.openid.conformance.support.mitre.compat.clients;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import net.openid.conformance.support.mitre.compat.model.RegisteredClient;
import net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity;
import net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity.AppType;
import net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity.AuthMethod;
import net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity.SubjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;

import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.APPLICATION_TYPE;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.CLAIMS_REDIRECT_URIS;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.CLIENT_ID;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.CLIENT_ID_ISSUED_AT;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.CLIENT_NAME;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.CLIENT_SECRET;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.CLIENT_SECRET_EXPIRES_AT;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.CLIENT_URI;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.CODE_CHALLENGE_METHOD;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.CONTACTS;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.DEFAULT_ACR_VALUES;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.DEFAULT_MAX_AGE;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.GRANT_TYPES;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.ID_TOKEN_ENCRYPTED_RESPONSE_ALG;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.ID_TOKEN_ENCRYPTED_RESPONSE_ENC;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.ID_TOKEN_SIGNED_RESPONSE_ALG;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.INITIATE_LOGIN_URI;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.JWKS;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.JWKS_URI;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.LOGO_URI;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.POLICY_URI;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.POST_LOGOUT_REDIRECT_URIS;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.REDIRECT_URIS;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.REGISTRATION_ACCESS_TOKEN;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.REGISTRATION_CLIENT_URI;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.REQUEST_OBJECT_SIGNING_ALG;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.REQUEST_URIS;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.REQUIRE_AUTH_TIME;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.RESPONSE_TYPES;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.SCOPE;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.SCOPE_SEPARATOR;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.SECTOR_IDENTIFIER_URI;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.SOFTWARE_ID;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.SOFTWARE_STATEMENT;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.SOFTWARE_VERSION;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.SUBJECT_TYPE;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.TOKEN_ENDPOINT_AUTH_METHOD;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.TOKEN_ENDPOINT_AUTH_SIGNING_ALG;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.TOS_URI;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.USERINFO_ENCRYPTED_RESPONSE_ALG;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.USERINFO_ENCRYPTED_RESPONSE_ENC;
import static net.openid.conformance.support.mitre.compat.clients.RegisteredClientFields.USERINFO_SIGNED_RESPONSE_ALG;
import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsArray;
import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsDate;
import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsJweAlgorithm;
import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsJweEncryptionMethod;
import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsJwsAlgorithm;
import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsPkceAlgorithm;
import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsString;
import static net.openid.conformance.support.mitre.compat.json.JsonUtils.getAsStringSet;

/**
 * Utility class to handle the parsing and serialization of ClientDetails objects.
 *
 * @author jricher
 */
public class ClientDetailsEntityJsonProcessor {

	private static final Logger logger = LoggerFactory.getLogger(ClientDetailsEntityJsonProcessor.class);

	/**
	 * Create an unbound ClientDetailsEntity from the given JSON string.
	 *
	 * @param jsonString
	 * @return the entity if successful, null otherwise
	 */
	public static ClientDetailsEntity parse(String jsonString) {
		JsonElement jsonEl = JsonParser.parseString(jsonString);
		return parse(jsonEl);
	}

	public static ClientDetailsEntity parse(JsonElement jsonEl) {
		if (jsonEl.isJsonObject()) {

			JsonObject o = jsonEl.getAsJsonObject();
			ClientDetailsEntity c = new ClientDetailsEntity();

			// these two fields should only be sent in the update request, and MUST match existing values
			c.setClientId(getAsString(o, CLIENT_ID));
			c.setClientSecret(getAsString(o, CLIENT_SECRET));

			// OAuth DynReg
			c.setRedirectUris(getAsStringSet(o, REDIRECT_URIS));
			c.setClientName(getAsString(o, CLIENT_NAME));
			c.setClientUri(getAsString(o, CLIENT_URI));
			c.setLogoUri(getAsString(o, LOGO_URI));
			c.setContacts(getAsStringSet(o, CONTACTS));
			c.setTosUri(getAsString(o, TOS_URI));

			String authMethod = getAsString(o, TOKEN_ENDPOINT_AUTH_METHOD);
			if (authMethod != null) {
				c.setTokenEndpointAuthMethod(AuthMethod.getByValue(authMethod));
			}

			// scope is a space-separated string
			String scope = getAsString(o, SCOPE);
			if (scope != null) {
				c.setScope(Sets.newHashSet(Splitter.on(SCOPE_SEPARATOR).split(scope)));
			}

			c.setGrantTypes(getAsStringSet(o, GRANT_TYPES));
			c.setResponseTypes(getAsStringSet(o, RESPONSE_TYPES));
			c.setPolicyUri(getAsString(o, POLICY_URI));
			c.setJwksUri(getAsString(o, JWKS_URI));

			JsonElement jwksEl = o.get(JWKS);
			if (jwksEl != null && jwksEl.isJsonObject()) {
				try {
					JWKSet jwks = JWKSet.parse(jwksEl.toString()); // we have to pass this through Nimbus's parser as a string
					c.setJwks(jwks);
				} catch (ParseException e) {
					logger.error("Unable to parse JWK Set for client", e);
					return null;
				}
			}

			// OIDC Additions
			String appType = getAsString(o, APPLICATION_TYPE);
			if (appType != null) {
				c.setApplicationType(AppType.getByValue(appType));
			}

			c.setSectorIdentifierUri(getAsString(o, SECTOR_IDENTIFIER_URI));

			String subjectType = getAsString(o, SUBJECT_TYPE);
			if (subjectType != null) {
				c.setSubjectType(SubjectType.getByValue(subjectType));
			}

			c.setRequestObjectSigningAlg(getAsJwsAlgorithm(o, REQUEST_OBJECT_SIGNING_ALG));

			c.setUserInfoSignedResponseAlg(getAsJwsAlgorithm(o, USERINFO_SIGNED_RESPONSE_ALG));
			c.setUserInfoEncryptedResponseAlg(getAsJweAlgorithm(o, USERINFO_ENCRYPTED_RESPONSE_ALG));
			c.setUserInfoEncryptedResponseEnc(getAsJweEncryptionMethod(o, USERINFO_ENCRYPTED_RESPONSE_ENC));

			c.setIdTokenSignedResponseAlg(getAsJwsAlgorithm(o, ID_TOKEN_SIGNED_RESPONSE_ALG));
			c.setIdTokenEncryptedResponseAlg(getAsJweAlgorithm(o, ID_TOKEN_ENCRYPTED_RESPONSE_ALG));
			c.setIdTokenEncryptedResponseEnc(getAsJweEncryptionMethod(o, ID_TOKEN_ENCRYPTED_RESPONSE_ENC));

			c.setTokenEndpointAuthSigningAlg(getAsJwsAlgorithm(o, TOKEN_ENDPOINT_AUTH_SIGNING_ALG));

			if (o.has(DEFAULT_MAX_AGE)) {
				if (o.get(DEFAULT_MAX_AGE).isJsonPrimitive()) {
					c.setDefaultMaxAge(o.get(DEFAULT_MAX_AGE).getAsInt());
				}
			}

			if (o.has(REQUIRE_AUTH_TIME)) {
				if (o.get(REQUIRE_AUTH_TIME).isJsonPrimitive()) {
					c.setRequireAuthTime(o.get(REQUIRE_AUTH_TIME).getAsBoolean());
				}
			}

			c.setDefaultACRvalues(getAsStringSet(o, DEFAULT_ACR_VALUES));
			c.setInitiateLoginUri(getAsString(o, INITIATE_LOGIN_URI));
			c.setPostLogoutRedirectUris(getAsStringSet(o, POST_LOGOUT_REDIRECT_URIS));
			c.setRequestUris(getAsStringSet(o, REQUEST_URIS));

			c.setClaimsRedirectUris(getAsStringSet(o, CLAIMS_REDIRECT_URIS));

			c.setCodeChallengeMethod(getAsPkceAlgorithm(o, CODE_CHALLENGE_METHOD));

			c.setSoftwareId(getAsString(o, SOFTWARE_ID));
			c.setSoftwareVersion(getAsString(o, SOFTWARE_VERSION));

			// note that this does not process or validate the software statement, that's handled in other components
			String softwareStatement = getAsString(o, SOFTWARE_STATEMENT);
			if (!Strings.isNullOrEmpty(softwareStatement)) {
				try {
					JWT softwareStatementJwt = JWTParser.parse(softwareStatement);
					c.setSoftwareStatement(softwareStatementJwt);
				} catch (ParseException e) {
					logger.warn("Error parsing software statement", e);
					return null;
				}
			}


			return c;
		} else {
			return null;
		}
	}

	/**
	 * Parse the JSON as a RegisteredClient (useful in the dynamic client filter)
	 */
	public static RegisteredClient parseRegistered(String jsonString) {


		JsonElement jsonEl = JsonParser.parseString(jsonString);
		return parseRegistered(jsonEl);
	}

	public static RegisteredClient parseRegistered(JsonElement jsonEl) {
		if (jsonEl.isJsonObject()) {

			JsonObject o = jsonEl.getAsJsonObject();
			ClientDetailsEntity c = parse(jsonEl);

			RegisteredClient rc = new RegisteredClient(c);
			// get any fields from the registration
			rc.setRegistrationAccessToken(getAsString(o, REGISTRATION_ACCESS_TOKEN));
			rc.setRegistrationClientUri(getAsString(o, REGISTRATION_CLIENT_URI));
			rc.setClientIdIssuedAt(getAsDate(o, CLIENT_ID_ISSUED_AT));
			rc.setClientSecretExpiresAt(getAsDate(o, CLIENT_SECRET_EXPIRES_AT));

			rc.setSource(o);

			return rc;
		} else {
			return null;
		}
	}

	/**
	 * @param c
	 * @return
	 */
	public static JsonObject serialize(RegisteredClient c) {

		if (c.getSource() != null) {
			// if we have the original object, just use that
			return c.getSource();
		} else {

			JsonObject o = new JsonObject();

			o.addProperty(CLIENT_ID, c.getClientId());
			if (c.getClientSecret() != null) {
				o.addProperty(CLIENT_SECRET, c.getClientSecret());

				if (c.getClientSecretExpiresAt() == null) {
					o.addProperty(CLIENT_SECRET_EXPIRES_AT, 0); // TODO: do we want to let secrets expire?
				} else {
					o.addProperty(CLIENT_SECRET_EXPIRES_AT, c.getClientSecretExpiresAt().getTime() / 1000L);
				}
			}

			if (c.getClientIdIssuedAt() != null) {
				o.addProperty(CLIENT_ID_ISSUED_AT, c.getClientIdIssuedAt().getTime() / 1000L);
			} else if (c.getCreatedAt() != null) {
				o.addProperty(CLIENT_ID_ISSUED_AT, c.getCreatedAt().getTime() / 1000L);
			}
			if (c.getRegistrationAccessToken() != null) {
				o.addProperty(REGISTRATION_ACCESS_TOKEN, c.getRegistrationAccessToken());
			}

			if (c.getRegistrationClientUri() != null) {
				o.addProperty(REGISTRATION_CLIENT_URI, c.getRegistrationClientUri());
			}


			// add in all other client properties

			// OAuth DynReg
			o.add(REDIRECT_URIS, getAsArray(c.getRedirectUris()));
			o.addProperty(CLIENT_NAME, c.getClientName());
			o.addProperty(CLIENT_URI, c.getClientUri());
			o.addProperty(LOGO_URI, c.getLogoUri());
			o.add(CONTACTS, getAsArray(c.getContacts()));
			o.addProperty(TOS_URI, c.getTosUri());
			o.addProperty(TOKEN_ENDPOINT_AUTH_METHOD, c.getTokenEndpointAuthMethod() != null ? c.getTokenEndpointAuthMethod().getValue() : null);
			o.addProperty(SCOPE, c.getScope() != null ? Joiner.on(SCOPE_SEPARATOR).join(c.getScope()) : null);
			o.add(GRANT_TYPES, getAsArray(c.getGrantTypes()));
			o.add(RESPONSE_TYPES, getAsArray(c.getResponseTypes()));
			o.addProperty(POLICY_URI, c.getPolicyUri());
			o.addProperty(JWKS_URI, c.getJwksUri());

			// get the JWKS sub-object
			if (c.getJwks() != null) {
				// We have to re-parse it into GSON because Nimbus uses a different parser
				JsonElement jwks = JsonParser.parseString(c.getJwks().toString());
				o.add(JWKS, jwks);
			} else {
				o.add(JWKS, null);
			}

			// OIDC Registration
			o.addProperty(APPLICATION_TYPE, c.getApplicationType() != null ? c.getApplicationType().getValue() : null);
			o.addProperty(SECTOR_IDENTIFIER_URI, c.getSectorIdentifierUri());
			o.addProperty(SUBJECT_TYPE, c.getSubjectType() != null ? c.getSubjectType().getValue() : null);
			o.addProperty(REQUEST_OBJECT_SIGNING_ALG, c.getRequestObjectSigningAlg() != null ? c.getRequestObjectSigningAlg().getName() : null);
			o.addProperty(USERINFO_SIGNED_RESPONSE_ALG, c.getUserInfoSignedResponseAlg() != null ? c.getUserInfoSignedResponseAlg().getName() : null);
			o.addProperty(USERINFO_ENCRYPTED_RESPONSE_ALG, c.getUserInfoEncryptedResponseAlg() != null ? c.getUserInfoEncryptedResponseAlg().getName() : null);
			o.addProperty(USERINFO_ENCRYPTED_RESPONSE_ENC, c.getUserInfoEncryptedResponseEnc() != null ? c.getUserInfoEncryptedResponseEnc().getName() : null);
			o.addProperty(ID_TOKEN_SIGNED_RESPONSE_ALG, c.getIdTokenSignedResponseAlg() != null ? c.getIdTokenSignedResponseAlg().getName() : null);
			o.addProperty(ID_TOKEN_ENCRYPTED_RESPONSE_ALG, c.getIdTokenEncryptedResponseAlg() != null ? c.getIdTokenEncryptedResponseAlg().getName() : null);
			o.addProperty(ID_TOKEN_ENCRYPTED_RESPONSE_ENC, c.getIdTokenEncryptedResponseEnc() != null ? c.getIdTokenEncryptedResponseEnc().getName() : null);
			o.addProperty(TOKEN_ENDPOINT_AUTH_SIGNING_ALG, c.getTokenEndpointAuthSigningAlg() != null ? c.getTokenEndpointAuthSigningAlg().getName() : null);
			o.addProperty(DEFAULT_MAX_AGE, c.getDefaultMaxAge());
			o.addProperty(REQUIRE_AUTH_TIME, c.getRequireAuthTime());
			o.add(DEFAULT_ACR_VALUES, getAsArray(c.getDefaultACRvalues()));
			o.addProperty(INITIATE_LOGIN_URI, c.getInitiateLoginUri());
			o.add(POST_LOGOUT_REDIRECT_URIS, getAsArray(c.getPostLogoutRedirectUris()));
			o.add(REQUEST_URIS, getAsArray(c.getRequestUris()));

			o.add(CLAIMS_REDIRECT_URIS, getAsArray(c.getClaimsRedirectUris()));

			o.addProperty(CODE_CHALLENGE_METHOD, c.getCodeChallengeMethod() != null ? c.getCodeChallengeMethod().getName() : null);

			o.addProperty(SOFTWARE_ID, c.getSoftwareId());
			o.addProperty(SOFTWARE_VERSION, c.getSoftwareVersion());

			if (c.getSoftwareStatement() != null) {
				o.addProperty(SOFTWARE_STATEMENT, c.getSoftwareStatement().serialize());
			}

			return o;
		}

	}
}
