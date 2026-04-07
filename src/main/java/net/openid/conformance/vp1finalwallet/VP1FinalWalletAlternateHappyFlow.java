package net.openid.conformance.vp1finalwallet;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddMismatchedIssToRequestObject;
import net.openid.conformance.condition.client.AddRandomParameterToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddVP1FinalEncryptionParametersToClientMetadata;
import net.openid.conformance.condition.client.AddVP1FinalEncryptionParametersToClientMetadataWithoutUseEnc;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicatesReorderedParams;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import org.jetbrains.annotations.NotNull;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-alternate-happy-flow",
	displayName = "OID4VP-1.0-FINAL: Alternate happy flow",
	summary = """
		Performs the normal flow with the following differences:
		- Includes optional 'state' parameter
		- Uses a longer 'nonce' (32 chars) and 'state' (64 chars)
		- Includes a random authorization endpoint parameter (which must be ignored)
		- Includes an 'iss' claim in the request object that does not match 'client_id' (which must be ignored as per VP spec section 5)
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

		// Per OID4VP 5.9.3, response_uri MAY be omitted when using redirect_uri prefix.
		// A dedicated test for that case could be added for the redirect_uri prefix variant.
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

	@NotNull
	@Override
	protected ConditionSequence createAuthorizationRedirectStepsUnsignedRequestUri() {
		// Including iss in an unsigned (alg: none) request object is unusual, but the spec says wallets
		// must ignore it regardless, so it's valid to test.
		return super.createAuthorizationRedirectStepsUnsignedRequestUri()
			.insertAfter(ConvertAuthorizationEndpointRequestToRequestObject.class,
				condition(AddMismatchedIssToRequestObject.class));
	}

	@NotNull
	@Override
	protected ConditionSequence createAuthorizationRedirectStepsSignedRequestUri() {
		return super.createAuthorizationRedirectStepsSignedRequestUri()
			.insertAfter(ConvertAuthorizationEndpointRequestToRequestObject.class,
				condition(AddMismatchedIssToRequestObject.class));
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
