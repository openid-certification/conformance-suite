package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddClaimsParameterSupportedTrueToServerConfiguration;
import net.openid.conformance.condition.as.AddSubjectTypesSupportedPairwiseToServerConfiguration;
import net.openid.conformance.condition.as.AustraliaConnectIdAddClaimsSupportedToServerConfiguration;
import net.openid.conformance.condition.as.AustraliaConnectIdAddTrustFrameworksSupportedToServerConfiguration;
import net.openid.conformance.condition.as.AustraliaConnectIdAddVerifiedClaimsToServerConfiguration;
import net.openid.conformance.condition.as.AustraliaConnectIdCheckForFAPI2ClaimsInRequestObject;
import net.openid.conformance.condition.as.AustraliaConnectIdCheckForUnexpectedParametersInPAREndpointRequest;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureAuthorizationRequestContainsNoUserinfoIdentityClaims;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureVerifiedClaimsInRequestObject;
import net.openid.conformance.condition.as.AustraliaConnectIdGenerateAccessTokenExpiration;
import net.openid.conformance.condition.as.AustraliaConnectIdValidatePurpose;
import net.openid.conformance.condition.as.AustraliaConnectIdValidateRequestObjectExp;
import net.openid.conformance.condition.as.AustraliaConnectIdValidateRequestObjectNBFClaim;
import net.openid.conformance.condition.as.AddFAPIInteractionIdToUserInfoEndpointResponse;
import net.openid.conformance.condition.as.EnsureRequestedScopeIsEqualToConfiguredScope;
import net.openid.conformance.condition.as.LoadRequestedIdTokenClaims;
import net.openid.conformance.condition.as.SetServerSigningAlgToPS256;
import net.openid.conformance.condition.as.ValidateFAPIInteractionIdInResourceRequest;
import net.openid.conformance.condition.client.AustraliaConnectIdEnsureAuthorizationRequestContainsNoAcrClaims;
import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenId;
import net.openid.conformance.condition.rs.ExtractFapiInteractionIdHeader;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

/**
 * Profile behavior for ConnectID Australia client tests.
 * Requires mTLS everywhere; uses userinfo as the resource endpoint (test ends there);
 * mandates FAPI interaction id at PAR / token / resource endpoints; adds verified-claims
 * and pairwise subject-types to server config; validates AU-specific request-object exp/nbf;
 * uses an AU-specific access-token expiration generator.
 */
public class ConnectIdAuClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public boolean userInfoIsResourceEndpoint() {
		return true;
	}

	@Override
	public boolean tokenEndpointRequiresIncomingRequest() {
		return true;
	}

	@Override
	public ConditionSequence addProfileSpecificServerConfiguration() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(SetServerSigningAlgToPS256.class, "CID-SP-4.2-8");
				callAndStopOnFailure(AddClaimsParameterSupportedTrueToServerConfiguration.class, "CID-SP-4");
				callAndStopOnFailure(AustraliaConnectIdAddClaimsSupportedToServerConfiguration.class, "CID-SP-4");
				callAndStopOnFailure(AustraliaConnectIdAddVerifiedClaimsToServerConfiguration.class, "IA-8", "CID-IDA-5.3.3");
				callAndStopOnFailure(AustraliaConnectIdAddTrustFrameworksSupportedToServerConfiguration.class, "IA-8", "CID-IDA-5.2-11");
				callAndStopOnFailure(AddSubjectTypesSupportedPairwiseToServerConfiguration.class, "CID-SP-4");
			}
		};
	}

	@Override
	public ConditionSequence addOidcSubjectTypesSupported() {
		// Pairwise subject types are added in addProfileSpecificServerConfiguration().
		return null;
	}

	@Override
	public ConditionSequence validateClientConfiguration() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(SetScopeInClientConfigurationToOpenId.class, "CID-SP-4");
			}
		};
	}

	@Override
	public ConditionSequence extractFapiInteractionIdHeader() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(ExtractFapiInteractionIdHeader.class, ConditionResult.FAILURE,
					"CID-SP-4.3-9", "FAPI2-IMP-2.1.1");
			}
		};
	}

	@Override
	public ConditionSequence additionalParRequestChecks() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(AustraliaConnectIdCheckForUnexpectedParametersInPAREndpointRequest.class,
					"CID-SP-4.3-5", "PAR-3");
			}
		};
	}

	@Override
	public ConditionSequence validateParRequestInteractionId() {
		return interactionIdSequence();
	}

	@Override
	public ConditionSequence validateTokenRequestInteractionId() {
		return interactionIdSequence();
	}

	private ConditionSequence interactionIdSequence() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(ExtractFapiInteractionIdHeader.class, ConditionResult.FAILURE,
					"CID-SP-4.3-9", "FAPI2-IMP-2.1.1");
				callAndContinueOnFailure(ValidateFAPIInteractionIdInResourceRequest.class, ConditionResult.FAILURE,
					"CID-SP-4.3-9", "FAPI2-IMP-2.1.1");
			}
		};
	}

	@Override
	public ConditionSequence additionalAuthorizationRequestChecks() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(AustraliaConnectIdCheckForFAPI2ClaimsInRequestObject.class)
					.skipIfElementMissing("authorization_request_object", "claims")
					.onSkip(ConditionResult.INFO)
					.onFail(ConditionResult.FAILURE)
					.requirements("CID-IDA-5.2-2"));
				call(condition(AustraliaConnectIdEnsureVerifiedClaimsInRequestObject.class)
					.skipIfElementMissing("authorization_request_object", "claims.claims.id_token.verified_claims.claims")
					.onSkip(ConditionResult.INFO)
					.onFail(ConditionResult.FAILURE)
					.requirements("CID-IDA-5.2-2"));
			}
		};
	}

	@Override
	public ConditionSequence validateRequestObjectExpNbf() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(AustraliaConnectIdValidateRequestObjectExp.class,
					"RFC7519-4.1.4", "CID-SP-4.3-7");
				callAndContinueOnFailure(AustraliaConnectIdValidateRequestObjectNBFClaim.class,
					ConditionResult.FAILURE, "CID-SP-4.3-8");
			}
		};
	}

	@Override
	public ConditionSequence validateAuthorizationRequestScope() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(AustraliaConnectIdEnsureAuthorizationRequestContainsNoUserinfoIdentityClaims.class,
					ConditionResult.FAILURE, "CID-SP-4");
				callAndContinueOnFailure(AustraliaConnectIdEnsureAuthorizationRequestContainsNoAcrClaims.class,
					ConditionResult.FAILURE, "CID-SP-4");
				callAndContinueOnFailure(AustraliaConnectIdValidatePurpose.class,
					ConditionResult.FAILURE, "CID-PURPOSE-6", "CID-IDA-5.2-10");
				callAndStopOnFailure(EnsureRequestedScopeIsEqualToConfiguredScope.class);
			}
		};
	}

	@Override
	public Class<? extends Condition> getGenerateAccessTokenExpirationCondition() {
		return AustraliaConnectIdGenerateAccessTokenExpiration.class;
	}

	@Override
	public ConditionSequence customizeIdTokenClaimsAfterHashes() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(LoadRequestedIdTokenClaims.class, ConditionResult.INFO);
			}
		};
	}

	@Override
	public ConditionSequence customizeUserInfoResponse() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(AddFAPIInteractionIdToUserInfoEndpointResponse.class, "CID-SP-4.3-9");
			}
		};
	}
}
