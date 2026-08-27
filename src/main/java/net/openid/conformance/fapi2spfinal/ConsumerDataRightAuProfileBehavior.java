package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddCdrXCdsClientHeadersToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddCdrXvToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIEndUserPresentFalseToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIEndUserPresentTrueToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToPAREndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddIpV6FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CdrCheckPARResponseExpiresIn;
import net.openid.conformance.condition.client.CdrValidateAccessTokenExpiresIn;
import net.openid.conformance.condition.client.CdrValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.CdrValidateJarmSigningAlg;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode;
import net.openid.conformance.condition.client.CheckDiscRequirePushedAuthorizationRequestsIsTrue;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInPARResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInTokenResponse;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureIdTokenDoesNotContainPersonalInformationClaims;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionIdPAREndpoint;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionIdTokenEndpoint;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsCDRAcrClaim;
import net.openid.conformance.condition.client.FAPIAuCdrCheckDiscEndpointClaimsSupported;
import net.openid.conformance.condition.client.RemoveCdrXCdsClientHeadersFromResourceEndpointRequest;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInServerJWKs;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.CDRAuthorizationEndpointSetup;

import java.util.function.Supplier;

/**
 * Profile behavior for Consumer Data Right Australia, as amended by Consultation
 * Draft 210 (adoption of FAPI 2.0).
 * Requires mTLS everywhere, CDR-specific headers on resource endpoints,
 * and CDR authorization endpoint setup.
 */
public class ConsumerDataRightAuProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public Class<? extends ConditionSequence> getProfileAuthorizationEndpointSetupSteps() {
		return CDRAuthorizationEndpointSetup.class;
	}

	@Override
	public ConditionSequence addResourceEndpointProfileHeaders(boolean isSecondClient) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// CDR requires x-fapi-end-user-present on all authenticated resource server endpoints
				if (isSecondClient) {
					// customer-not-present call; x-cds-client-headers must not be sent
					callAndStopOnFailure(AddFAPIEndUserPresentFalseToResourceEndpointRequest.class, "CDR-http-headers");
				} else {
					callAndStopOnFailure(AddFAPIEndUserPresentTrueToResourceEndpointRequest.class, "CDR-http-headers");
					// CDR requires this header for customer present calls
					callAndStopOnFailure(AddCdrXCdsClientHeadersToResourceEndpointRequest.class, "CDR-http-headers");
				}

				// Data Recipients MAY send x-fapi-interaction-id; send it for both clients so the
				// mandatory x-fapi-interaction-id in the response can be checked as an echo
				callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
				callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "CDR-http-headers");

				callAndStopOnFailure(AddCdrXvToResourceEndpointRequest.class, "CDR-http-headers");
			}
		};
	}

	@Override
	public ConditionSequence addParEndpointProfileHeaders() {
		return createFapiInteractionIdHeaderSequence(
			AddFAPIInteractionIdToPAREndpointRequest.class, "CDR-http-headers");
	}

	@Override
	public ConditionSequence addTokenEndpointProfileHeaders() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// Data Recipients SHOULD reuse the same x-fapi-interaction-id value across the PAR
				// request and the token endpoint request, so no new interaction id is created here
				callAndStopOnFailure(AddFAPIInteractionIdToTokenEndpointRequest.class, "CDR-http-headers");
			}
		};
	}

	@Override
	public ConditionSequence validateParResponseProfileHeaders() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(CheckForFAPIInteractionIdInPARResponse.class)
					.skipIfElementMissing("pushed_authorization_endpoint_response_headers", "x-fapi-interaction-id")
					.onSkip(ConditionResult.FAILURE)
					.onFail(ConditionResult.FAILURE)
					.requirements("CDR-http-headers")
					.dontStopOnFailure());
				call(condition(EnsureMatchingFAPIInteractionIdPAREndpoint.class)
					.skipIfElementMissing("pushed_authorization_endpoint_response_headers", "x-fapi-interaction-id")
					.onSkip(ConditionResult.FAILURE)
					.onFail(ConditionResult.FAILURE)
					.requirements("CDR-http-headers")
					.dontStopOnFailure());
				call(condition(CdrCheckPARResponseExpiresIn.class)
					.onFail(ConditionResult.FAILURE)
					.requirements("CDR-request-object")
					.dontStopOnFailure());
			}
		};
	}

	@Override
	public ConditionSequence validateTokenEndpointResponseInteractionId() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(CheckForFAPIInteractionIdInTokenResponse.class)
					.skipIfElementMissing("token_endpoint_response_headers", "x-fapi-interaction-id")
					.onSkip(ConditionResult.FAILURE)
					.onFail(ConditionResult.FAILURE)
					.requirements("CDR-http-headers")
					.dontStopOnFailure());
				call(condition(EnsureMatchingFAPIInteractionIdTokenEndpoint.class)
					.skipIfElementMissing("token_endpoint_response_headers", "x-fapi-interaction-id")
					.onSkip(ConditionResult.FAILURE)
					.onFail(ConditionResult.FAILURE)
					.requirements("CDR-http-headers")
					.dontStopOnFailure());
			}
		};
	}

	@Override
	public ConditionSequence validateResourceEndpointResponseHeaders(boolean isSecondClient) {
		// x-fapi-interaction-id is mandatory in resource responses for CDR
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(CheckForFAPIInteractionIdInResourceResponse.class)
					.skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id")
					.onSkip(ConditionResult.FAILURE)
					.onFail(ConditionResult.FAILURE)
					.requirements("CDR-http-headers")
					.dontStopOnFailure());
				call(condition(EnsureMatchingFAPIInteractionId.class)
					.skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id")
					.onSkip(ConditionResult.FAILURE)
					.onFail(ConditionResult.FAILURE)
					.requirements("CDR-http-headers")
					.dontStopOnFailure());
			}
		};
	}

	@Override
	public ConditionSequence validateExpiresIn() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(CdrValidateAccessTokenExpiresIn.class)
					.skipIfObjectMissing("expires_in")
					.onSkip(ConditionResult.INFO)
					.onFail(ConditionResult.FAILURE)
					.requirement("CDR-tokens")
					.dontStopOnFailure());
			}
		};
	}

	@Override
	public ConditionSequence validateIdTokenSigningAlg() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(CdrValidateIdTokenSigningAlg.class,
					ConditionResult.FAILURE, "CDR-tokens");
			}
		};
	}

	@Override
	public ConditionSequence validateJarmSigningAlg() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(CdrValidateJarmSigningAlg.class,
					ConditionResult.FAILURE, "CDR-authentication-flows");
			}
		};
	}

	@Override
	public ConditionSequence validateIdTokenEncryption() {
		// not encryption related, but this hook runs at the right point for
		// CDR's additional profile-specific id_token checks
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(EnsureIdTokenDoesNotContainPersonalInformationClaims.class,
					ConditionResult.FAILURE, "CDR-tokens");
			}
		};
	}

	@Override
	public ConditionSequence onConfigure() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(CheckDistinctKeyIdValueInServerJWKs.class,
					ConditionResult.FAILURE, "CDR-security-endpoints");
			}
		};
	}


	@Override
	public ConditionSequence addAlternateResourceEndpointProfileHeaders() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// exercise a customer-not-present call; x-cds-client-headers is only sent when the customer is present
				callAndStopOnFailure(AddFAPIEndUserPresentFalseToResourceEndpointRequest.class, "CDR-http-headers");
				callAndStopOnFailure(RemoveCdrXCdsClientHeadersFromResourceEndpointRequest.class, "CDR-http-headers");
				// x-fapi-customer-ip-address is no longer part of the CDR standards, but Data Holders
				// are still required to ignore it, so keep sending it
				callAndStopOnFailure(AddIpV6FapiCustomerIpAddressToResourceEndpointRequest.class, "CDR-http-headers");
			}
		};
	}

	@Override
	public Supplier<? extends ConditionSequence> getProfileSpecificDiscoveryChecks() {
		return DiscoveryEndpointChecks::new;
	}

	public static class DiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			// claims parameter support is required in Australia
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, ConditionResult.FAILURE, "OIDCD-3", "CID-IDA-5.1");
			callAndContinueOnFailure(FAPIAuCdrCheckDiscEndpointClaimsSupported.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureServerConfigurationSupportsCDRAcrClaim.class, ConditionResult.WARNING);
			callAndContinueOnFailure(CheckDiscRequirePushedAuthorizationRequestsIsTrue.class, ConditionResult.FAILURE, "CDR-security-endpoints");
		}
	}
}
