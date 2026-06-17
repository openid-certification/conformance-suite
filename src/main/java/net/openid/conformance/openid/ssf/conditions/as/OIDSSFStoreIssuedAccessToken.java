package net.openid.conformance.openid.ssf.conditions.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

/**
 * Persists a freshly minted access token in the {@code ssf.issued_tokens} map,
 * keyed by token value, so {@code OIDSSFHandleAuthorizationHeader} can look it
 * up and validate expiry on subsequent SSF API requests in
 * {@link net.openid.conformance.openid.ssf.variant.SsfAuthMode#DYNAMIC} mode.
 * <p>
 * Each record holds the {@code client_id} the token was issued to, the granted
 * {@code scope}, and the absolute {@code expires_at} epoch-second.
 */
public class OIDSSFStoreIssuedAccessToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf", "client"}, strings = {"access_token", "access_token_expiration"})
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("access_token");
		long expiresIn = Long.parseLong(env.getString("access_token_expiration"));
		long expiresAt = Instant.now().getEpochSecond() + expiresIn;

		String clientId = env.getString("client", "client_id");
		String scope = env.getString("scope");

		JsonObject tokenRecord = new JsonObject();
		tokenRecord.addProperty("client_id", clientId);
		if (scope != null) {
			tokenRecord.addProperty("scope", scope);
		}
		tokenRecord.addProperty("expires_at", expiresAt);

		JsonObject issuedTokens;
		JsonElement issuedTokensEl = env.getElementFromObject("ssf", "issued_tokens");
		if (issuedTokensEl != null && issuedTokensEl.isJsonObject()) {
			issuedTokens = issuedTokensEl.getAsJsonObject();
		} else {
			issuedTokens = new JsonObject();
			env.putObject("ssf", "issued_tokens", issuedTokens);
		}
		issuedTokens.add(accessToken, tokenRecord);

		logSuccess("Stored issued access token", args("token_record", tokenRecord));

		return env;
	}
}
