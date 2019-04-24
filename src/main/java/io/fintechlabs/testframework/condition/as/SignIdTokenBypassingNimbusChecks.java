package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;


public class SignIdTokenBypassingNimbusChecks extends SignIdToken {

	public SignIdTokenBypassingNimbusChecks(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = {"id_token_claims", "server_jwks"})
	@PostEnvironment(strings = "id_token")
	protected String performSigning(JWSHeader header, JsonObject claims, JWSSigner signer) {

		try {
			Payload payload = new Payload(claims.toString());

			JWSObject jwsObject = new JWSObject(header, payload);

			jwsObject.sign(signer);

			return jwsObject.serialize();

		} catch (
			JOSEException e) {
			throw error(e);
		}

	}

}
