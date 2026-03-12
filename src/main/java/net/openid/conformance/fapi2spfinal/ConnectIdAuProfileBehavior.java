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
import net.openid.conformance.sequence.ConditionSequence;

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
	public void setupResourceEndpoint() {
		// ConnectID always uses the MTLS userinfo endpoint
		module.doCallAndStopOnFailure(SetProtectedResourceUrlToMtlsUserInfoEndpoint.class, "CID-SP-4");
	}

	@Override
	public void configureClientScope() {
		module.doCallAndStopOnFailure(SetScopeInClientConfigurationToOpenId.class);
	}

	@Override
	public void addParEndpointProfileHeaders() {
		addFapiInteractionIdHeader(
			"pushed_authorization_request_endpoint_request_headers",
			AddFAPIInteractionIdToPAREndpointRequest.class,
			"CID-SP-4.2-12", "CDR-http-headers");
	}

	@Override
	public void customizeAuthorizationRequestSteps(ConditionSequence seq) {
		seq.then(module.doCondition(ConnectIdAddPurposeToAuthorizationEndpointRequest.class)
			.requirements("CID-PURPOSE-4", "CID-IDA-5.2-10"));
	}

	@Override
	public void addTokenEndpointProfileHeaders() {
		addFapiInteractionIdHeader(
			"token_endpoint_request_headers",
			AddFAPIInteractionIdToTokenEndpointRequest.class,
			"CID-SP-4.2-12", "CDR-http-headers");
	}

	@Override
	public void validateExpiresIn() {
		module.doSkipIfMissing(new String[]{"expires_in"}, null, ConditionResult.INFO,
			AustraliaConnectIdValidateAccessTokenExpiresIn.class, ConditionResult.FAILURE,
			"CID-SP-4.2-2");
	}

	@Override
	public void validateTokenEndpointResponseInteractionId() {
		module.doSkipIfElementMissing("token_endpoint_response_headers",
			"x-fapi-interaction-id", ConditionResult.FAILURE,
			CheckForFAPIInteractionIdInTokenResponse.class,
			ConditionResult.FAILURE, "CID_SP-4.2-12", "FAPI2-IMP-2.1.1");
		module.doSkipIfElementMissing("token_endpoint_response_headers",
			"x-fapi-interaction-id", ConditionResult.FAILURE,
			EnsureMatchingFAPIInteractionIdTokenEndpoint.class,
			ConditionResult.FAILURE, "CID_SP-4.2-12", "FAPI2-IMP-2.1.1");
	}

	@Override
	public void addResourceEndpointProfileHeaders(boolean isSecondClient) {
		if (!isSecondClient) {
			// Optional headers for first client
			module.doCallAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "CDR-http-headers");
			module.doCallAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "CDR-http-headers");
		}
		// Mandatory interaction ID for ConnectID on all requests
		module.doCallAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		module.doCallAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class,
			"CID-SP-4.2-12", "CDR-http-headers");
	}

	@Override
	public void validateResourceEndpointResponseHeaders(boolean isSecondClient) {
		// Mandatory for ConnectID - use FAILURE skip result
		module.doSkipIfElementMissing("resource_endpoint_response_headers",
			"x-fapi-interaction-id", ConditionResult.FAILURE,
			CheckForFAPIInteractionIdInResourceResponse.class,
			ConditionResult.FAILURE, "CID-SP-4.2-12", "FAPI2-IMP-2.1.1");
		module.doSkipIfElementMissing("resource_endpoint_response_headers",
			"x-fapi-interaction-id", ConditionResult.FAILURE,
			EnsureMatchingFAPIInteractionId.class,
			ConditionResult.FAILURE, "CID-SP-4.2-12", "FAPI2-IMP-2.1.1");
	}

	@Override
	public void validateParResponseProfileHeaders() {
		module.doSkipIfElementMissing("pushed_authorization_endpoint_response_headers",
			"x-fapi-interaction-id", ConditionResult.FAILURE,
			CheckForFAPIInteractionIdInPARResponse.class,
			ConditionResult.FAILURE, "CID-SP-4.2-12", "FAPI2-IMP-2.1.1");
		module.doSkipIfElementMissing("pushed_authorization_endpoint_response_headers",
			"x-fapi-interaction-id", ConditionResult.FAILURE,
			EnsureMatchingFAPIInteractionIdPAREndpoint.class,
			ConditionResult.FAILURE, "CID-SP-4.2-12", "FAPI2-IMP-2.1.1");
	}
}
