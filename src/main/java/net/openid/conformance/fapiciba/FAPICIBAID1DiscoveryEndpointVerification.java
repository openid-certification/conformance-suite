package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckBackchannelTokenDeliveryPingModeSupported;
import net.openid.conformance.condition.client.CheckBackchannelTokenDeliveryPollModeSupported;
import net.openid.conformance.condition.client.CheckBackchannelUserCodeParameterSupported;
import net.openid.conformance.condition.client.CheckDiscBackchannelAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckDiscEndpointBackchannelAuthenticationRequestSigningAlgValuesSupported;
import net.openid.conformance.condition.client.FAPICIBACheckDiscEndpointGrantTypesSupported;
import net.openid.conformance.fapirwid2.AbstractFAPIDiscoveryEndpointVerification;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi-ciba-id1-discovery-end-point-verification",
	displayName = "FAPI-CIBA-ID1: Discovery Endpoint Verification",
	summary = "This test ensures that the server's discovery document (including token_delivery_modes, response_types, grant_types etc) contains correct values.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.discoveryUrl",
	}
)
@VariantParameters({
	CIBAMode.class,
	FAPI1FinalOPProfile.class
})
@VariantNotApplicable(parameter = CIBAMode.class, values = { "push" })
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = { "openbanking_brazil", "openinsurance_brazil" })
public class FAPICIBAID1DiscoveryEndpointVerification extends AbstractFAPIDiscoveryEndpointVerification {
	private Class<? extends ConditionSequence> variantModeChecks;

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

	@VariantSetup(parameter = CIBAMode.class, value = "ping")
	public void setupPing() {
		variantModeChecks = PingChecks.class;
	}

	@VariantSetup(parameter = CIBAMode.class, value = "poll")
	public void setupPoll() {
		variantModeChecks = PollChecks.class;
	}

	@Override
	protected void performEndpointVerification() {
		super.performEndpointVerification();

		callAndContinueOnFailure(CheckDiscBackchannelAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-4");
		callAndContinueOnFailure(CheckDiscEndpointBackchannelAuthenticationRequestSigningAlgValuesSupported.class, Condition.ConditionResult.WARNING, "CIBA-4");
		callAndContinueOnFailure(CheckBackchannelUserCodeParameterSupported.class, Condition.ConditionResult.WARNING, "CIBA-4");
		callAndContinueOnFailure(FAPICIBACheckDiscEndpointGrantTypesSupported.class, Condition.ConditionResult.FAILURE, "CIBA-4");

		performProfileSpecificChecks();

		call(sequence(variantModeChecks));
	}

	public void performProfileSpecificChecks() {
	}
}
