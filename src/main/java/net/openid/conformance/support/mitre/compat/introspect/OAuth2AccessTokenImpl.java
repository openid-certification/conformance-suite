package net.openid.conformance.support.mitre.compat.introspect;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.io.Serial;
import java.time.Instant;
import java.util.Set;

@SuppressWarnings("serial")
public class OAuth2AccessTokenImpl extends OAuth2AccessToken {

	@Serial
	private static final long serialVersionUID = 1L;

	private JsonObject introspectionResponse;

	@SuppressWarnings("this-escape")
	public OAuth2AccessTokenImpl(JsonObject introspectionResponse, String tokenString) {
		super(TokenType.BEARER, tokenString, issuedAt(introspectionResponse), extractExpiresAt(introspectionResponse), extractScopes(introspectionResponse));
		setIntrospectionResponse(introspectionResponse);
	}

	private static Instant extractExpiresAt(JsonObject introspectionResponse) {
		if (introspectionResponse.get("exp") != null) {
			return Instant.ofEpochMilli(introspectionResponse.get("exp").getAsLong() * 1000L);
		}
		return null;
	}

	private static Set<String> extractScopes(JsonObject introspectionResponse) {
		if (introspectionResponse.get("scope") != null) {
			return Sets.newHashSet(Splitter.on(" ").split(introspectionResponse.get("scope").getAsString()));
		}
		return Set.of();
	}

	private static Instant issuedAt(JsonObject introspectionResponse) {
		if (introspectionResponse.get("iat") != null) {
			return Instant.ofEpochSecond(Long.parseLong(introspectionResponse.get("iat").toString()));
		}
		return Instant.now();
	}

	/**
	 * @return the token
	 */
	public JsonObject getIntrospectionResponse() {
		return introspectionResponse;
	}


	/**
	 * @param token the token to set
	 */
	public void setIntrospectionResponse(JsonObject token) {
		this.introspectionResponse = token;
	}

}
