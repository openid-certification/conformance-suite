package net.openid.conformance.vp1finalwallet;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddRandomParameterToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddVP1FinalEncryptionParametersToClientMetadata;
import net.openid.conformance.condition.client.AddVP1FinalEncryptionParametersToClientMetadataWithoutUseEnc;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicatesReorderedParams;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-alternate-happy-flow",
	displayName = "OID4VP-1.0-FINAL: Alternate happy flow",
	summary = """
		Performs the normal flow with the following differences:
		- Includes optional 'state' parameter
		- Uses a longer 'nonce' (32 chars) and 'state' (64 chars)
		- Includes a random authorization endpoint parameter (which must be ignored)
		- Reordered query parameters in the redirect URL
		- Encryption key without 'use: enc' (for encrypted response modes)
		- response_uri response returns a redirect_uri which the wallet must open""",
	profile = "OID4VP-1FINAL"
)

public class VP1FinalWalletAlternateHappyFlow extends AbstractVP1FinalWalletTest {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		callAndStopOnFailure(CreateRedirectUri.class);
		// try a longer nonce
		env.putInteger("requested_nonce_length", 32);

		// also use a longer state value than is used by default
		env.putInteger("requested_state_length", 64);

		// FIXME: is response_uri is optional when using the redirect_uri scheme; if so we should omit it in this test: https://github.com/openid/OpenID4VP/issues/93
	}

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence createAuthorizationRequestSteps = super.createAuthorizationRequestSequence();

		createAuthorizationRequestSteps = createAuthorizationRequestSteps.
			then(condition(AddRandomParameterToAuthorizationEndpointRequest.class));

		createAuthorizationRequestSteps.replace(AddVP1FinalEncryptionParametersToClientMetadata.class,
			condition(AddVP1FinalEncryptionParametersToClientMetadataWithoutUseEnc.class));

		return createAuthorizationRequestSteps;
	}

	@Override
	protected Class<? extends Condition> getRequestUriRedirectCondition() {
		return BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicatesReorderedParams.class;
	}

	@Override
	protected void populateDirectPostResponse() {
		super.populateDirectPostResponseWithRedirectUri();
	}
}
