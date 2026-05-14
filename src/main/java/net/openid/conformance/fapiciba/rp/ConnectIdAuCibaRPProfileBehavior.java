package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddClaimsParameterSupportedTrueToServerConfiguration;
import net.openid.conformance.condition.as.AddFAPIInteractionIdToUserInfoEndpointResponse;
import net.openid.conformance.condition.as.AddSubjectTypesSupportedPairwiseToServerConfiguration;
import net.openid.conformance.condition.as.AustraliaConnectIdAddClaimsSupportedToServerConfiguration;
import net.openid.conformance.condition.as.AustraliaConnectIdAddTrustFrameworksSupportedToServerConfiguration;
import net.openid.conformance.condition.as.AustraliaConnectIdAddTxnToIdTokenClaims;
import net.openid.conformance.condition.as.AustraliaConnectIdAddVerifiedClaimsToServerConfiguration;
import net.openid.conformance.condition.as.AustraliaConnectIdCheckForFAPI2ClaimsInRequestObject;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureRequestObjectContainsNoAcrClaims;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureRequestObjectContainsTrustFramework;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureRequestObjectSigningAlgIsPS256;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureVerifiedClaimsInRequestObject;
import net.openid.conformance.condition.as.AustraliaConnectIdValidateRequestObjectBindingMessage;
import net.openid.conformance.condition.as.AustraliaConnectIdValidateRequestObjectExp;
import net.openid.conformance.condition.as.AustraliaConnectIdValidateRequestObjectNBFClaim;
import net.openid.conformance.condition.as.AustraliaConnectIdWarnIfRequestObjectBindingMessageIsNotAscii;
import net.openid.conformance.condition.as.CreateFapiInteractionIdIfNeeded;
import net.openid.conformance.condition.as.ExtractServerSigningAlg;
import net.openid.conformance.condition.as.FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.LoadRequestedIdTokenClaims;
import net.openid.conformance.condition.as.par.AddPushedAuthorizationRequestEndpointToServerConfig;
import net.openid.conformance.condition.rs.ExtractFapiInteractionIdHeader;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

public class ConnectIdAuCibaRPProfileBehavior extends FAPICIBARPProfileBehavior {

	@Override
	public ConditionSequence applyProfileSpecificServerConfigurationSetup() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(ConnectIdCibaSetBackchannelTokenDeliveryModesSupportedToPollOnly.class,
					"CID-CIBA-4.2-1");
				callAndStopOnFailure(ExtractServerSigningAlg.class);
				callAndStopOnFailure(AddPushedAuthorizationRequestEndpointToServerConfig.class,
					"CID-SP-4.2-7");
				callAndStopOnFailure(AddClaimsParameterSupportedTrueToServerConfiguration.class,
					"CID-IDA-5.1-1", "CID-CIBA-4.2-2");
				callAndStopOnFailure(AustraliaConnectIdAddClaimsSupportedToServerConfiguration.class,
					"CID-IDA-5.1-3");
				callAndStopOnFailure(AustraliaConnectIdAddVerifiedClaimsToServerConfiguration.class,
					"CID-IDA-5.3.3");
				callAndStopOnFailure(AustraliaConnectIdAddTrustFrameworksSupportedToServerConfiguration.class,
					"CID-IDA-5.1-11");
				callAndStopOnFailure(AddSubjectTypesSupportedPairwiseToServerConfiguration.class,
					"CID-IDA-5.1-4");
			}
		};
	}

	@Override
	public ConditionSequence applyProfileSpecificServerAuthAlgSetup() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
				callAndStopOnFailure(ConnectIdCibaSetBackchannelAuthenticationRequestSigningAlgValuesSupportedToPS256Only.class,
					"CID-SP-4.2-8");
			}
		};
	}

	@Override
	public void exposeProfileSpecificEndpoints() {
		module.exposeMtlsPath("userinfo_endpoint", "userinfo");
	}

	@Override
	public boolean userInfoEndpointRequiresMTLS() {
		return true;
	}

	@Override
	public ConditionSequence prepareNonResourceEndpointFapiInteractionId() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(ExtractFapiInteractionIdHeader.class)
					.skipIfElementMissing("incoming_request", "headers.x-fapi-interaction-id")
					.onSkip(Condition.ConditionResult.INFO)
					.onFail(Condition.ConditionResult.FAILURE)
					.requirements("CID-SP-4.3-9")
					.dontStopOnFailure());
				callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "CID-SP-4.3-9");
			}
		};
	}

	@Override
	public ConditionSequence addFapiInteractionIdToTokenEndpointResponse() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(AddFAPIInteractionIdToTokenEndpointResponse.class, "CID-SP-4.3-9");
			}
		};
	}

	@Override
	public ConditionSequence addFapiInteractionIdToBackchannelEndpointResponse() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(AddFAPIInteractionIdToBackchannelEndpointResponse.class, "CID-SP-4.3-9");
			}
		};
	}

	@Override
	public ConditionSequence addFapiInteractionIdToUserInfoEndpointResponse() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(AddFAPIInteractionIdToUserInfoEndpointResponse.class, "CID-SP-4.3-9");
			}
		};
	}

	@Override
	public ConditionSequence applyProfileSpecificBackchannelRequestChecks() {
		ConditionSequence defaultChecks = super.applyProfileSpecificBackchannelRequestChecks();
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(defaultChecks);
				callAndStopOnFailure(AustraliaConnectIdEnsureRequestObjectSigningAlgIsPS256.class, "CID-SP-4.2-8");
				callAndStopOnFailure(AustraliaConnectIdValidateRequestObjectExp.class, "CID-SP-4.2-10");
				callAndStopOnFailure(AustraliaConnectIdValidateRequestObjectNBFClaim.class, "CID-SP-4.2-11");
				callAndStopOnFailure(AustraliaConnectIdValidateRequestObjectBindingMessage.class, "CID-IDA-5.2-10");
				callAndContinueOnFailure(AustraliaConnectIdWarnIfRequestObjectBindingMessageIsNotAscii.class, Condition.ConditionResult.WARNING, "CID-IDA-5.2-10");
				callAndStopOnFailure(AustraliaConnectIdEnsureRequestObjectContainsNoAcrClaims.class, "CID-IDA-5.2-5");
				callAndStopOnFailure(AustraliaConnectIdCheckForFAPI2ClaimsInRequestObject.class, "CID-IDA-5.2-7");
				callAndStopOnFailure(AustraliaConnectIdEnsureVerifiedClaimsInRequestObject.class, "CID-IDA-5.2-11");
				callAndStopOnFailure(AustraliaConnectIdEnsureRequestObjectContainsTrustFramework.class, "CID-IDA-5.1-11");
			}
		};
	}

	@Override
	public ConditionSequence applyProfileSpecificIdTokenClaims() {
		ConditionSequence defaultClaims = super.applyProfileSpecificIdTokenClaims();
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(defaultClaims);
				callAndStopOnFailure(LoadRequestedIdTokenClaims.class);
				callAndStopOnFailure(AustraliaConnectIdAddTxnToIdTokenClaims.class, "CID-IDA-5.1.6");
			}
		};
	}
}
