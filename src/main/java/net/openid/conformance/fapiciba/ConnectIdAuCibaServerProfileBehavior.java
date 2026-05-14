package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckClaimsSupported;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckTrustFrameworkSupported;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckVerifiedClaimsSupported;
import net.openid.conformance.condition.client.AustraliaConnectIdEnsureMtlsAliasesContainsRequiredEndpoints;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointIdTokenSigningAlgValuesSupportedContainsPS256;
import net.openid.conformance.condition.client.CheckDiscEndpointSubjectTypesSupportedContainsOnlyPairwise;
import net.openid.conformance.condition.client.CheckDiscEndpointUserinfoEndpoint;
import net.openid.conformance.condition.client.ConnectIdCibaCheckBackchannelAuthenticationRequestSigningAlgValuesSupportedContainsOnlyPS256;
import net.openid.conformance.condition.client.ConnectIdCibaCheckBackchannelTokenDeliveryModesSupportedOnlyPoll;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.function.Supplier;

public class ConnectIdAuCibaServerProfileBehavior extends FAPICIBAServerProfileBehavior {

	@Override
	public Supplier<? extends ConditionSequence> getProfileSpecificDiscoveryChecks() {
		return DiscoveryEndpointChecks::new;
	}

	public static class DiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(ConnectIdCibaCheckBackchannelTokenDeliveryModesSupportedOnlyPoll.class,
				Condition.ConditionResult.FAILURE, "CID-CIBA-4.2-1");
			callAndContinueOnFailure(ConnectIdCibaCheckBackchannelAuthenticationRequestSigningAlgValuesSupportedContainsOnlyPS256.class,
				Condition.ConditionResult.FAILURE, "CID-SP-4.2-8");
			callAndContinueOnFailure(AustraliaConnectIdEnsureMtlsAliasesContainsRequiredEndpoints.class,
				Condition.ConditionResult.FAILURE, "CID-SP-4.2-7");
			callAndContinueOnFailure(CheckDiscEndpointIdTokenSigningAlgValuesSupportedContainsPS256.class,
				Condition.ConditionResult.FAILURE, "OIDCD-3", "CID-SP-5");
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class,
				Condition.ConditionResult.FAILURE, "OIDCD-3", "CID-IDA-5.1-1", "CID-CIBA-4.2-2");
			callAndContinueOnFailure(AustraliaConnectIdCheckClaimsSupported.class,
				Condition.ConditionResult.FAILURE, "OIDCD-3", "CID-IDA-5.1-3");
			callAndContinueOnFailure(AustraliaConnectIdCheckVerifiedClaimsSupported.class,
				Condition.ConditionResult.INFO, "CID-IDA-5.3.3");
			callAndContinueOnFailure(AustraliaConnectIdCheckTrustFrameworkSupported.class,
				Condition.ConditionResult.INFO, "CID-IDA-5.1-11");
			callAndContinueOnFailure(CheckDiscEndpointSubjectTypesSupportedContainsOnlyPairwise.class,
				Condition.ConditionResult.FAILURE, "CID-IDA-5.1-4");
			callAndContinueOnFailure(CheckDiscEndpointUserinfoEndpoint.class,
				Condition.ConditionResult.FAILURE, "CID-SP-4.2-3");
		}
	}
}
