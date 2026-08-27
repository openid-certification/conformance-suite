package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddClaimsParameterSupportedTrueToServerConfiguration;
import net.openid.conformance.condition.as.CdrAddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.CdrAddAcrValuesSupportedToServerConfiguration;
import net.openid.conformance.condition.as.CdrAddCdrArrangementIdToTokenEndpointResponse;
import net.openid.conformance.condition.as.CdrAddClaimsSupportedToServerConfiguration;
import net.openid.conformance.condition.as.CdrCheckForUnexpectedClaimsInClaimsParameter;
import net.openid.conformance.condition.as.CdrGenerateAccessTokenExpiration;
import net.openid.conformance.condition.as.CdrEnsureTokenRequestInteractionIdMatchesParRequest;
import net.openid.conformance.condition.as.CdrRecordParRequestInteractionId;
import net.openid.conformance.condition.as.CdrValidateAuthorizationSignedResponseAlg;
import net.openid.conformance.condition.as.CdrValidateRequestObjectSharingDuration;
import net.openid.conformance.condition.as.CreateFapiInteractionIdIfNeeded;
import net.openid.conformance.condition.as.ExtractServerSigningAlg;
import net.openid.conformance.condition.as.par.CdrCreatePAREndpointResponse;
import net.openid.conformance.condition.client.CdrValidateRequestObjectIdTokenACRClaims;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.rs.CdrValidateFapiEndUserPresentHeader;
import net.openid.conformance.condition.rs.ExtractFapiInteractionIdHeader;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

/**
 * Profile behavior for Consumer Data Right Australia client tests, as amended by
 * Consultation Draft 210 (adoption of FAPI 2.0).
 * Requires mTLS everywhere; validates the CDR-specific headers and request object
 * claims Data Recipients send, and emulates a CDR Data Holder (x-fapi-interaction-id
 * on responses, cdr_arrangement_id in token responses, no refresh token rotation).
 */
public class ConsumerDataRightAuClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public boolean shouldRotateRefreshTokens() {
		// CDR Data Holders MUST NOT support refresh token rotation
		return false;
	}

	@Override
	public boolean tokenEndpointRequiresIncomingRequest() {
		// the x-fapi-interaction-id checks in validateTokenRequestInteractionId() read the
		// token request's headers from incoming_request
		return true;
	}

	@Override
	public ConditionSequence addProfileSpecificServerConfiguration() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(ExtractServerSigningAlg.class);
				callAndStopOnFailure(AddClaimsParameterSupportedTrueToServerConfiguration.class, "CDR-security-endpoints");
				callAndStopOnFailure(CdrAddClaimsSupportedToServerConfiguration.class, "CDR-security-endpoints");
				callAndStopOnFailure(CdrAddAcrValuesSupportedToServerConfiguration.class, "CDR-levels-of-assurance-loas");
			}
		};
	}

	@Override
	public ConditionSequence validateIdTokenAcrClaims() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(CdrValidateRequestObjectIdTokenACRClaims.class)
					.onFail(ConditionResult.INFO)
					.requirements("CDR-levels-of-assurance-loas", "OIDCC-5.5.1.1")
					.dontStopOnFailure());
			}
		};
	}

	@Override
	public Class<? extends Condition> getAddAcrClaimToIdTokenClaimsCondition() {
		return CdrAddACRClaimToIdTokenClaims.class;
	}

	@Override
	public Class<? extends Condition> getCheckForUnexpectedClaimsInClaimsParameterCondition() {
		return CdrCheckForUnexpectedClaimsInClaimsParameter.class;
	}

	@Override
	public Class<? extends Condition> getGenerateAccessTokenExpirationCondition() {
		return CdrGenerateAccessTokenExpiration.class;
	}

	@Override
	public Class<? extends Condition> getCreatePAREndpointResponseCondition() {
		return CdrCreatePAREndpointResponse.class;
	}

	@Override
	public ConditionSequence validateClientConfiguration() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class,
					ConditionResult.FAILURE, "CDR-security-endpoints");
				callAndContinueOnFailure(CdrValidateAuthorizationSignedResponseAlg.class,
					ConditionResult.FAILURE, "CDR-authentication-flows");
			}
		};
	}

	@Override
	public ConditionSequence validateParRequestInteractionId() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// Data Recipients MAY send x-fapi-interaction-id; extract it so the response echoes it
				call(condition(ExtractFapiInteractionIdHeader.class)
					.skipIfElementMissing("incoming_request", "headers.x-fapi-interaction-id")
					.onSkip(ConditionResult.INFO)
					.onFail(ConditionResult.FAILURE)
					.requirements("CDR-http-headers")
					.dontStopOnFailure());
				callAndStopOnFailure(CdrRecordParRequestInteractionId.class, "CDR-http-headers");
				// the Data Holder must respond with x-fapi-interaction-id even when none was sent
				callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "CDR-http-headers");
			}
		};
	}

	@Override
	public ConditionSequence validateTokenRequestInteractionId() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(CdrEnsureTokenRequestInteractionIdMatchesParRequest.class)
					.skipIfElementMissing("incoming_request", "headers.x-fapi-interaction-id")
					.onSkip(ConditionResult.INFO)
					.onFail(ConditionResult.WARNING)
					.requirements("CDR-http-headers")
					.dontStopOnFailure());
				call(condition(ExtractFapiInteractionIdHeader.class)
					.skipIfElementMissing("incoming_request", "headers.x-fapi-interaction-id")
					.onSkip(ConditionResult.INFO)
					.onFail(ConditionResult.FAILURE)
					.requirements("CDR-http-headers")
					.dontStopOnFailure());
				callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "CDR-http-headers");
			}
		};
	}

	@Override
	public ConditionSequence additionalAuthorizationRequestChecks() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(CdrValidateRequestObjectSharingDuration.class,
					ConditionResult.FAILURE, "CDR-request-object");
			}
		};
	}

	@Override
	public ConditionSequence validateAccountsEndpointRequest() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(CdrValidateFapiEndUserPresentHeader.class,
					ConditionResult.FAILURE, "CDR-http-headers");
			}
		};
	}

	@Override
	public ConditionSequence customizeTokenEndpointResponse() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(CdrAddCdrArrangementIdToTokenEndpointResponse.class, "CDR-tokens");
			}
		};
	}
}
