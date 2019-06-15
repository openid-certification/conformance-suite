package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckBackchannelTokenDeliveryPollModeSupported;
import io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsMTLS;
import io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsPrivateKeyJwt;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-poll-discovery-end-point-verification",
	displayName = "FAPI-CIBA: Poll Mode Discovery Endpoint Verification",
	summary = "This test ensures that the server's discovery document (including token_delivery_modes, response_types, grant_types etc) contains correct values.",
	profile = "FAPI-CIBA",
	configurationFields = {
		"server.discoveryUrl",
	}
)
public class FAPICIBAPollDiscoveryEndpointVerification extends AbstractFAPICIBADiscoveryEndpointVerification {
	private Class<? extends ConditionSequence> variantChecks;

	public static class MtlsChecks extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndContinueOnFailure(EnsureServerConfigurationSupportsMTLS.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");

		}
	}
	public static class PrivateKeyJWTChecks extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndContinueOnFailure(EnsureServerConfigurationSupportsPrivateKeyJwt.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");

		}
	}

	@Variant(name = FAPICIBA.variant_poll_mtls)
	public void setupMTLS() {
		variantChecks = MtlsChecks.class;
	}
	@Variant(name = FAPICIBA.variant_poll_privatekeyjwt)
	public void setupPrivateKeyJwt() {
		variantChecks = PrivateKeyJWTChecks.class;
	}

	@Override
	protected void performEndpointVerification() {
		super.performEndpointVerification();
		callAndContinueOnFailure(CheckBackchannelTokenDeliveryPollModeSupported.class, Condition.ConditionResult.FAILURE, "CIBA-4");
		call(sequence(variantChecks));
	}

	@Override
	public void performProfileSpecificChecks() {
	}

}
