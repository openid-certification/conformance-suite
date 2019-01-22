package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

/**
 * @author jricher
 *
 */
public class EnsureTLSEndpoints extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		call(exec().startBlock("Authorization endpoint TLS test")
			.mapKey("tls", "authorization_endpoint_tls"));

		call(condition(EnsureTLS12.class)
			.onFail(ConditionResult.FAILURE)
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowTLS10.class)
			.onFail(ConditionResult.FAILURE)
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowTLS11.class)
			.onFail(ConditionResult.FAILURE)
			.requirement("FAPI-RW-8.5-2"));

		call(exec().startBlock("Token Endpoint TLS test")
			.mapKey("tls", "token_endpoint_tls"));
		call(condition(EnsureTLS12.class)
			.onFail(ConditionResult.FAILURE)
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowTLS10.class)
			.onFail(ConditionResult.FAILURE)
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowTLS11.class)
			.onFail(ConditionResult.FAILURE)
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowInsecureCipher.class)
			.onFail(ConditionResult.FAILURE)
			.requirement("FAPI-RW-8.5-2"));

		call(exec().startBlock("Userinfo Endpoint TLS test")
			.mapKey("tls", "userinfo_endpoint_tls"));
		call(condition(EnsureTLS12.class)
			.onFail(ConditionResult.FAILURE)
			.onSkip(ConditionResult.INFO)
			.skipIfObjectMissing("tls")
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowTLS10.class)
			.onFail(ConditionResult.FAILURE)
			.onSkip(ConditionResult.INFO)
			.skipIfObjectMissing("tls")
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowTLS11.class)
			.onFail(ConditionResult.FAILURE)
			.onSkip(ConditionResult.INFO)
			.skipIfObjectMissing("tls")
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowInsecureCipher.class)
			.onFail(ConditionResult.FAILURE)
			.onSkip(ConditionResult.INFO)
			.skipIfObjectMissing("tls")
			.requirement("FAPI-RW-8.5-2"));


		call(exec().startBlock("Registration Endpoint TLS test")
			.mapKey("tls", "registration_endpoint_tls"));
		call(condition(EnsureTLS12.class)
			.onFail(ConditionResult.FAILURE)
			.onSkip(ConditionResult.INFO)
			.skipIfObjectMissing("tls")
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowTLS10.class)
			.onFail(ConditionResult.FAILURE)
			.onSkip(ConditionResult.INFO)
			.skipIfObjectMissing("tls")
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowTLS11.class)
			.onFail(ConditionResult.FAILURE)
			.onSkip(ConditionResult.INFO)
			.skipIfObjectMissing("tls")
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowInsecureCipher.class)
			.onFail(ConditionResult.FAILURE)
			.onSkip(ConditionResult.INFO)
			.skipIfObjectMissing("tls")
			.requirement("FAPI-RW-8.5-2"));

		call(exec().startBlock("Resource Endpoint TLS test")
			.mapKey("tls", "resource_endpoint_tls"));
		call(condition(EnsureTLS12.class)
			.onFail(ConditionResult.FAILURE)
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowTLS10.class)
			.onFail(ConditionResult.FAILURE)
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowTLS11.class)
			.onFail(ConditionResult.FAILURE)
			.requirement("FAPI-RW-8.5-2"));
		call(condition(DisallowInsecureCipher.class)
			.onFail(ConditionResult.FAILURE)
			.requirement("FAPI-RW-8.5-2"));

		call(exec().endBlock()
			.unmapKey("tls"));

	}

}
