package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddUserinfoSignedResponseAlgRS256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CheckDiscEndpointUserinfoSigningAlgValuesSupportedContainsRS256;
import net.openid.conformance.condition.client.EnsureContentTypeApplicationJwt;
import net.openid.conformance.condition.client.EnsureUserInfoDoesNotContainNonce;
import net.openid.conformance.condition.client.ExtractSignedUserInfoFromUserInfoEndpointResponse;
import net.openid.conformance.condition.client.ValidateSignedUserInfoResponseStandardJWTClaims;
import net.openid.conformance.condition.client.ValidateUserInfoResponseSignature;
import net.openid.conformance.condition.client.ValidateUserInfoSigningAlgIsRS256;
import net.openid.conformance.sequence.client.OIDCCCreateDynamicClientRegistrationRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantNotApplicable;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_UserInfo_RS256
@PublishTestModule(
	testName = "oidcc-userinfo-rs256",
	displayName = "OIDCC: Userinfo RS256 ",
	summary = "This tests register a client with userinfo_signed_response_alg=RS256 and validates the signed response from the userinfo endpoint",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ClientRegistration.class, values = { "static_client" })
public class OIDCCUserInfoRS256 extends AbstractOIDCCUserInfoTest {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		callAndContinueOnFailure(CheckDiscEndpointUserinfoSigningAlgValuesSupportedContainsRS256.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		call(new OIDCCCreateDynamicClientRegistrationRequest(responseType)
			.then(condition(AddUserinfoSignedResponseAlgRS256ToDynamicRegistrationRequest.class).requirement("OIDCR-2"))
		);

		expose("client_name", env.getString("dynamic_registration_request", "client_name"));
	}

	@Override
	protected void extractUserInfoResponse() {
		call(exec().mapKey("endpoint_response", "userinfo_endpoint_response_full"));
		callAndContinueOnFailure(EnsureContentTypeApplicationJwt.class, Condition.ConditionResult.FAILURE, "OIDCC-5.3.2");
		call(exec().unmapKey("endpoint_response"));
		callAndContinueOnFailure(ValidateUserInfoResponseSignature.class, Condition.ConditionResult.FAILURE, "OIDCC-5.3.2");
		// should probably also use AbstractVerifyJwsSignatureUsingKid at some point
		callAndStopOnFailure(ExtractSignedUserInfoFromUserInfoEndpointResponse.class);
		callAndContinueOnFailure(ValidateUserInfoSigningAlgIsRS256.class, Condition.ConditionResult.FAILURE);

		// This is just a warning since https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse is fairly
		// lax on what is required
		callAndContinueOnFailure(ValidateSignedUserInfoResponseStandardJWTClaims.class, Condition.ConditionResult.WARNING, "OIDCC-5.3.2");

		// This is not a 'must not' in the spec, but equally including nonce here is almost certainly a mistake by the
		// implementor there is nothing in the spec that suggests including nonce
		callAndContinueOnFailure(EnsureUserInfoDoesNotContainNonce.class, Condition.ConditionResult.FAILURE, "OIDCC-5.3.2", "OIDCC-5.1");
	}

}
