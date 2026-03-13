package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToPAREndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIpV4FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.AustraliaConnectIdValidateAccessTokenExpiresIn;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInPARResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInTokenResponse;
import net.openid.conformance.condition.client.ConnectIdAddPurposeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionIdPAREndpoint;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionIdTokenEndpoint;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToMtlsUserInfoEndpoint;
import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenId;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;

/**
 * Profile behavior for ConnectID Australia.
 * Requires mTLS everywhere, uses userinfo endpoint as resource, mandates FAPI interaction IDs
 * on all endpoints, adds purpose claim to authorization requests, and validates ConnectID-specific
 * access token expiry.
 */
public class ConnectIdAuProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public ConditionSequence setupResourceEndpoint() {
		// ConnectID always uses the MTLS userinfo endpoint
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(SetProtectedResourceUrlToMtlsUserInfoEndpoint.class, "CID-SP-4");
			}
		};
	}

	@Override
	public ConditionSequence configureClientScope() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(SetScopeInClientConfigurationToOpenId.class);
			}
		};
	}

	@Override
	public ConditionSequence addParEndpointProfileHeaders() {
		return createFapiInteractionIdHeaderSequence(
			AddFAPIInteractionIdToPAREndpointRequest.class,
			"CID-SP-4.2-12", "CDR-http-headers");
	}

	@Override
	public void customizeAuthorizationRequestSteps(ConditionSequence seq) {
		seq.then(new ConditionCallBuilder(ConnectIdAddPurposeToAuthorizationEndpointRequest.class)
			.requirements("CID-PURPOSE-4", "CID-IDA-5.2-10"));
	}

	@Override
	public ConditionSequence addTokenEndpointProfileHeaders() {
		return createFapiInteractionIdHeaderSequence(
			AddFAPIInteractionIdToTokenEndpointRequest.class,
			"CID-SP-4.2-12", "CDR-http-headers");
	}

	@Override
	public ConditionSequence validateExpiresIn() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(AustraliaConnectIdValidateAccessTokenExpiresIn.class)
					.skipIfObjectMissing("expires_in")
					.onSkip(ConditionResult.INFO)
					.onFail(ConditionResult.FAILURE)
					.requirement("CID-SP-4.2-2")
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
					.requirements("CID-SP-4.2-12", "FAPI2-IMP-2.1.1")
					.dontStopOnFailure());
				call(condition(EnsureMatchingFAPIInteractionIdTokenEndpoint.class)
					.skipIfElementMissing("token_endpoint_response_headers", "x-fapi-interaction-id")
					.onSkip(ConditionResult.FAILURE)
					.onFail(ConditionResult.FAILURE)
					.requirements("CID-SP-4.2-12", "FAPI2-IMP-2.1.1")
					.dontStopOnFailure());
			}
		};
	}

	@Override
	public ConditionSequence addResourceEndpointProfileHeaders(boolean isSecondClient) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				if (!isSecondClient) {
					// Optional headers for first client
					callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "CDR-http-headers");
					callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "CDR-http-headers");
				}
				// Mandatory interaction ID for ConnectID on all requests
				callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
				callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class,
					"CID-SP-4.2-12", "CDR-http-headers");
			}
		};
	}

	@Override
	public ConditionSequence validateResourceEndpointResponseHeaders(boolean isSecondClient) {
		// Mandatory for ConnectID - use FAILURE skip result
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(CheckForFAPIInteractionIdInResourceResponse.class)
					.skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id")
					.onSkip(ConditionResult.FAILURE)
					.onFail(ConditionResult.FAILURE)
					.requirements("CID-SP-4.2-12", "FAPI2-IMP-2.1.1")
					.dontStopOnFailure());
				call(condition(EnsureMatchingFAPIInteractionId.class)
					.skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id")
					.onSkip(ConditionResult.FAILURE)
					.onFail(ConditionResult.FAILURE)
					.requirements("CID-SP-4.2-12", "FAPI2-IMP-2.1.1")
					.dontStopOnFailure());
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
					.requirements("CID-SP-4.2-12", "FAPI2-IMP-2.1.1")
					.dontStopOnFailure());
				call(condition(EnsureMatchingFAPIInteractionIdPAREndpoint.class)
					.skipIfElementMissing("pushed_authorization_endpoint_response_headers", "x-fapi-interaction-id")
					.onSkip(ConditionResult.FAILURE)
					.onFail(ConditionResult.FAILURE)
					.requirements("CID-SP-4.2-12", "FAPI2-IMP-2.1.1")
					.dontStopOnFailure());
			}
		};
	}
}
