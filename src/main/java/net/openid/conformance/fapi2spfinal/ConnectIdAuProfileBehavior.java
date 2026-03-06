package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToPAREndpointRequest;
import net.openid.conformance.condition.client.AddIpV4FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToTokenEndpointRequest;
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
 * Australian ConnectID profile behavior.
 */
public class ConnectIdAuProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public void setupResourceEndpoint(AbstractFAPI2SPFinalServerTestModule module) {
		// always use the MTLS version if available, as ConnectID always uses mtls sender constraining
		module.doCallAndStopOnFailure(SetProtectedResourceUrlToMtlsUserInfoEndpoint.class, "CID-SP-4");
	}

	@Override
	public void configureClientScope(AbstractFAPI2SPFinalServerTestModule module) {
		module.doCallAndStopOnFailure(SetScopeInClientConfigurationToOpenId.class);
	}

	@Override
	public void addParEndpointProfileHeaders(AbstractFAPI2SPFinalServerTestModule module) {
		module.doCallAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		if (module.getEnv().getObject("pushed_authorization_request_endpoint_request_headers") == null) {
			module.getEnv().putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());
		}
		module.doCallAndStopOnFailure(AddFAPIInteractionIdToPAREndpointRequest.class, "CID-SP-4.2-12", "CDR-http-headers");
	}

	@Override
	public void validateParResponseProfileHeaders(AbstractFAPI2SPFinalServerTestModule module) {
		module.doSkipIfElementMissing("pushed_authorization_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.FAILURE,
			CheckForFAPIInteractionIdInPARResponse.class, ConditionResult.FAILURE, "CID-SP-4.2-12", "FAPI2-IMP-2.1.1");
		module.doSkipIfElementMissing("pushed_authorization_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.FAILURE,
			EnsureMatchingFAPIInteractionIdPAREndpoint.class, ConditionResult.FAILURE, "CID-SP-4.2-12", "FAPI2-IMP-2.1.1");
	}

	@Override
	public void customizeAuthorizationRequest(ConditionSequence seq, AbstractFAPI2SPFinalServerTestModule module) {
		seq.then(module.doCondition(ConnectIdAddPurposeToAuthorizationEndpointRequest.class)
			.requirements("CID-PURPOSE-4", "CID-IDA-5.2-10"));
	}

	@Override
	public void addTokenEndpointProfileHeaders(AbstractFAPI2SPFinalServerTestModule module) {
		module.doCallAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		if (module.getEnv().getObject("token_endpoint_request_headers") == null) {
			module.getEnv().putObject("token_endpoint_request_headers", new JsonObject());
		}
		module.doCallAndStopOnFailure(AddFAPIInteractionIdToTokenEndpointRequest.class, "CID-SP-4.2-12", "CDR-http-headers");
	}

	@Override
	public void validateTokenExpiresIn(AbstractFAPI2SPFinalServerTestModule module) {
		module.doSkipIfMissing(new String[]{"expires_in"}, null, ConditionResult.INFO,
			AustraliaConnectIdValidateAccessTokenExpiresIn.class, ConditionResult.FAILURE, "CID-SP-4.2-2");
	}

	@Override
	public void validateTokenResponseProfileHeaders(AbstractFAPI2SPFinalServerTestModule module) {
		module.doSkipIfElementMissing("token_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.FAILURE,
			CheckForFAPIInteractionIdInTokenResponse.class, ConditionResult.FAILURE, "CID_SP-4.2-12", "FAPI2-IMP-2.1.1");
		module.doSkipIfElementMissing("token_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.FAILURE,
			EnsureMatchingFAPIInteractionIdTokenEndpoint.class, ConditionResult.FAILURE, "CID_SP-4.2-12", "FAPI2-IMP-2.1.1");
	}

	@Override
	public void addResourceEndpointProfileHeaders(AbstractFAPI2SPFinalServerTestModule module, boolean isSecondClient) {
		if (!isSecondClient) {
			// these are optional; only add them for the first client
			module.doCallAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "CDR-http-headers");
			module.doCallAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "CDR-http-headers");
		}
		// Mandatory for ConnectID for both clients
		module.doCallAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		module.doCallAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "CID-SP-4.2-12", "CDR-http-headers");
	}

	@Override
	public void validateResourceResponseProfileHeaders(AbstractFAPI2SPFinalServerTestModule module, boolean isSecondClient) {
		// Mandatory for ConnectID for both clients
		module.doSkipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.FAILURE,
			CheckForFAPIInteractionIdInResourceResponse.class, ConditionResult.FAILURE, "CID-SP-4.2-12", "FAPI2-IMP-2.1.1");
		module.doSkipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.FAILURE,
			EnsureMatchingFAPIInteractionId.class, ConditionResult.FAILURE, "CID-SP-4.2-12", "FAPI2-IMP-2.1.1");
	}

	@Override
	public ConditionResult interactionIdSkipResult() {
		return ConditionResult.FAILURE;
	}
}
