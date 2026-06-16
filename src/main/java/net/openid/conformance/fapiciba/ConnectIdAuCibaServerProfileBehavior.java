package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddEssentialTxnClaimRequestToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToBackchannelAuthenticationEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToTokenEndpointRequest;
import net.openid.conformance.condition.client.AustraliaConnectIdAddClaimsToAuthorizationEndpointRequestIdTokenClaims;
import net.openid.conformance.condition.client.AustraliaConnectIdAddVerifiedClaimsToAuthorizationEndpointRequestIdTokenClaims;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckClaimsSupported;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckTrustFrameworkSupported;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckVerifiedClaimsSupported;
import net.openid.conformance.condition.client.AustraliaConnectIdEnsureIdTokenContainsTxn;
import net.openid.conformance.condition.client.AustraliaConnectIdEnsureIdTokenContainsTrustFramework;
import net.openid.conformance.condition.client.AustraliaConnectIdEnsureIdTokenContainsVerifiedClaims;
import net.openid.conformance.condition.client.AustraliaConnectIdEnsureIdTokenDoesNotContainAcr;
import net.openid.conformance.condition.client.AustraliaConnectIdEnsureMtlsAliasesContainsRequiredEndpoints;
import net.openid.conformance.condition.client.AustraliaConnectIdValidateAccessTokenExpiresIn;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointIdTokenSigningAlgValuesSupportedContainsPS256;
import net.openid.conformance.condition.client.CheckDiscEndpointSubjectTypesSupportedContainsOnlyPairwise;
import net.openid.conformance.condition.client.CheckDiscEndpointUserinfoEndpoint;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInBackchannelAuthenticationResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInTokenResponse;
import net.openid.conformance.condition.client.ConnectIdCibaCheckBackchannelAuthenticationRequestSigningAlgValuesSupportedContainsOnlyPS256;
import net.openid.conformance.condition.client.ConnectIdCibaCheckBackchannelTokenDeliveryModesSupportedOnlyPoll;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureIdTokenContainsRequestedClaims;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionIdBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionIdTokenEndpoint;
import net.openid.conformance.condition.client.FAPIValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.SetConnectIdBindingMessageToPurpose;
import net.openid.conformance.condition.client.SetConnectIdCibaLoginHintFromConfiguration;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.function.Supplier;

public class ConnectIdAuCibaServerProfileBehavior extends FAPICIBAServerProfileBehavior {

	@Override
	public Supplier<? extends ConditionSequence> getProfileSpecificDiscoveryChecks() {
		return DiscoveryEndpointChecks::new;
	}

	@Override
	public Class<? extends ConditionSequence> getProfileAuthorizationEndpointSetupSteps() {
		return AuthorizationEndpointSetupSteps.class;
	}

	@Override
	public Class<? extends ConditionSequence> getProfileIdTokenValidationSteps() {
		return IdTokenValidationSteps.class;
	}

	@Override
	public ConditionSequence addBackchannelAuthenticationEndpointProfileHeaders() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
				callAndStopOnFailure(AddFAPIInteractionIdToBackchannelAuthenticationEndpointRequest.class,
					"CID-SP-4.3-9");
			}
		};
	}

	@Override
	public ConditionSequence validateBackchannelAuthenticationEndpointResponseHeaders() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(CheckForFAPIInteractionIdInBackchannelAuthenticationResponse.class,
					Condition.ConditionResult.FAILURE, "CID-SP-4.2-12");
				callAndContinueOnFailure(EnsureMatchingFAPIInteractionIdBackchannelAuthenticationEndpoint.class,
					Condition.ConditionResult.FAILURE, "CID-SP-4.2-12");
			}
		};
	}

	@Override
	public ConditionSequence addTokenEndpointProfileHeaders() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
				callAndStopOnFailure(AddFAPIInteractionIdToTokenEndpointRequest.class,
					"CID-SP-4.2-12", "CID-SP-4.3-9");
			}
		};
	}

	@Override
	public ConditionSequence addResourceEndpointProfileHeaders(boolean isSecondClient) {
		if (isSecondClient) {
			return null;
		}
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);
				callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
				callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class,
					"CID-SP-4.3-9");
			}
		};
	}

	@Override
	public ConditionSequence validateResourceEndpointResponseHeaders(boolean isSecondClient) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class,
					Condition.ConditionResult.FAILURE, "CID-SP-4.4-1");
				if (!isSecondClient) {
					callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class,
						Condition.ConditionResult.FAILURE, "CID-SP-4.4-1");
				}
			}
		};
	}

	@Override
	public ConditionSequence validateTokenEndpointResponseHeaders() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(CheckForFAPIInteractionIdInTokenResponse.class,
					Condition.ConditionResult.FAILURE, "CID-SP-4.2-12");
				callAndContinueOnFailure(EnsureMatchingFAPIInteractionIdTokenEndpoint.class,
					Condition.ConditionResult.FAILURE, "CID-SP-4.2-12");
			}
		};
	}

	@Override
	public ConditionSequence validateExpiresIn() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(AustraliaConnectIdValidateAccessTokenExpiresIn.class)
					.skipIfObjectMissing("expires_in")
					.onSkip(Condition.ConditionResult.INFO)
					.onFail(Condition.ConditionResult.FAILURE)
					.requirement("CID-SP-4.2-2")
					.dontStopOnFailure());
			}
		};
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

	public static class AuthorizationEndpointSetupSteps extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(SetConnectIdCibaLoginHintFromConfiguration.class,
				"CID-CIBA-4.1.1.1", "CID-CIBA-4.1.2.1", "CID-CIBA-4.1.3.1", "CID-CIBA-4.3-1");
			call(new CommonAuthorizationEndpointSetupSteps());
		}
	}

	public static class CommonAuthorizationEndpointSetupSteps extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(SetConnectIdBindingMessageToPurpose.class, "CID-IDA-5.2-10");
			callAndStopOnFailure(AustraliaConnectIdAddClaimsToAuthorizationEndpointRequestIdTokenClaims.class,
				"CID-CIBA-4.3-3", "CID-IDA-5.2-4", "CID-IDA-5.2-6");
			callAndStopOnFailure(AddEssentialTxnClaimRequestToAuthorizationEndpointRequest.class,
				"CID-IDA-5.2-7");
			callAndStopOnFailure(AustraliaConnectIdAddVerifiedClaimsToAuthorizationEndpointRequestIdTokenClaims.class,
				"CID-CIBA-4.3-3", "CID-IDA-5.2-11");
		}
	}

	public static class IdTokenValidationSteps extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class,
				Condition.ConditionResult.FAILURE, "FAPI-RW-8.6", "FAPI1-ADV-8.6");
			callAndContinueOnFailure(EnsureIdTokenContainsRequestedClaims.class,
				Condition.ConditionResult.WARNING, "OIDCC-5.5", "CID-IDA-5.1-2.9");
			callAndContinueOnFailure(AustraliaConnectIdEnsureIdTokenContainsTxn.class,
				Condition.ConditionResult.FAILURE, "CID-IDA-5.1-2.6");
			callAndContinueOnFailure(AustraliaConnectIdEnsureIdTokenDoesNotContainAcr.class,
				Condition.ConditionResult.FAILURE, "CID-IDA-5.1-2.2");
			callAndContinueOnFailure(AustraliaConnectIdEnsureIdTokenContainsTrustFramework.class,
				Condition.ConditionResult.FAILURE, "CID-IDA-5.1-11", "CID-IDA-5.2-12");
			callAndContinueOnFailure(AustraliaConnectIdEnsureIdTokenContainsVerifiedClaims.class,
				Condition.ConditionResult.FAILURE, "CID-IDA-5.2-12");
		}
	}
}
