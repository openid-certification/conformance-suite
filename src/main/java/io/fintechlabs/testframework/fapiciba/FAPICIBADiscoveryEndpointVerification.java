package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckBackchannelTokenDeliveryPingModeSupported;
import io.fintechlabs.testframework.condition.client.CheckBackchannelTokenDeliveryPollModeSupported;
import io.fintechlabs.testframework.condition.client.CheckBackchannelUserCodeParameterSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscBackchannelAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointBackchannelAuthenticationRequestSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsMTLS;
import io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsPrivateKeyJwt;
import io.fintechlabs.testframework.condition.client.FAPICIBACheckDiscEndpointGrantTypesSupported;
import io.fintechlabs.testframework.fapi.AbstractFAPIDiscoveryEndpointVerification;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-discovery-end-point-verification",
	displayName = "FAPI-CIBA: Discovery Endpoint Verification",
	summary = "This test ensures that the server's discovery document (including token_delivery_modes, response_types, grant_types etc) contains correct values.",
	profile = "FAPI-CIBA",
	configurationFields = {
		"server.discoveryUrl",
	}
)
public class FAPICIBADiscoveryEndpointVerification extends AbstractFAPIDiscoveryEndpointVerification {
	private Class<? extends ConditionSequence> variantAuthChecks;
	private Class<? extends ConditionSequence> variantModeChecks;

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
	public static class PollChecks extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckBackchannelTokenDeliveryPollModeSupported.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");

		}
	}
	public static class PingChecks extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckBackchannelTokenDeliveryPingModeSupported.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");

		}
	}

	@Variant(name = FAPICIBA.variant_poll_mtls)
	public void setupPollMTLS() {
		variantAuthChecks = MtlsChecks.class;
		variantModeChecks = PollChecks.class;
	}
	@Variant(name = FAPICIBA.variant_poll_privatekeyjwt)
	public void setupPollPrivateKeyJwt() {
		variantAuthChecks = PrivateKeyJWTChecks.class;
		variantModeChecks = PollChecks.class;
	}
	@Variant(name = FAPICIBA.variant_ping_mtls)
	public void setupPingMTLS() {
		variantAuthChecks = MtlsChecks.class;
		variantModeChecks = PingChecks.class;
	}
	@Variant(name = FAPICIBA.variant_ping_privatekeyjwt)
	public void setupPingPrivateKeyJwt() {
		variantAuthChecks = PrivateKeyJWTChecks.class;
		variantModeChecks = PingChecks.class;
	}

	@Override
	protected void performEndpointVerification() {
		super.performEndpointVerification();

		callAndContinueOnFailure(CheckDiscBackchannelAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-4");
		callAndContinueOnFailure(CheckDiscEndpointBackchannelAuthenticationRequestSigningAlgValuesSupported.class, Condition.ConditionResult.WARNING, "CIBA-4");
		callAndContinueOnFailure(CheckBackchannelUserCodeParameterSupported.class, Condition.ConditionResult.WARNING, "CIBA-4");
		callAndContinueOnFailure(FAPICIBACheckDiscEndpointGrantTypesSupported.class, Condition.ConditionResult.FAILURE, "CIBA-4");

		performProfileSpecificChecks();

		call(sequence(variantAuthChecks));
		call(sequence(variantModeChecks));
	}

	public void performProfileSpecificChecks() {
	}

}
