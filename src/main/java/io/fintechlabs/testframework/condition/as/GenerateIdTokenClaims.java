package io.fintechlabs.testframework.condition.as;

import java.time.Instant;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class GenerateIdTokenClaims extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public GenerateIdTokenClaims(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = { "user_info", "client" }, strings = "issuer")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		String subject = env.getString("user_info", "sub");
		String issuer = env.getString("issuer");
		String clientId = env.getString("client", "client_id");
		String nonce = env.getString("nonce");

		if (Strings.isNullOrEmpty(subject)) {
			throw error("Couldn't find subject");
		}

		if (Strings.isNullOrEmpty(issuer)) {
			throw error("Couldn't find issuer");
		}

		if (Strings.isNullOrEmpty(clientId)) {
			throw error("Couldn't find client ID");
		}

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", issuer);
		claims.addProperty("sub", subject);
		claims.addProperty("aud", clientId);

		if (!Strings.isNullOrEmpty(nonce)) {
			claims.addProperty("nonce", nonce);
		}

		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(5 * 60);

		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());

		env.putObject("id_token_claims", claims);

		logSuccess("Created ID Token Claims", claims);

		return env;

	}

}
