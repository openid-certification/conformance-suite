package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.ExtractCHash;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractSHash;
import io.fintechlabs.testframework.condition.client.ValidateCHash;
import io.fintechlabs.testframework.condition.client.ValidateSHash;

/**
 * This class uses to perform only steps that call to token endpoint
 */
public abstract class AbstractFAPIRWID2PerformTokenEndpoint extends AbstractFAPIRWID2ServerTestModule {

	protected void performPostAuthorizationFlow() {

		callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI-RW-5.2.2-3");

		// save the id_token returned from the authorisation endpoint
		env.putObject("authorization_endpoint_id_token", env.getObject("id_token"));

		performIdTokenValidation();

		callAndContinueOnFailure(ExtractSHash.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		skipIfMissing(new String[]{"s_hash"}, null, Condition.ConditionResult.INFO,
			ValidateSHash.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		callAndContinueOnFailure(ExtractCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		skipIfMissing(new String[]{"c_hash"}, null, Condition.ConditionResult.INFO,
			ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		// call the token endpoint and complete the flow
		createAuthorizationCodeRequest();

		requestAuthorizationCode();

	}

}
