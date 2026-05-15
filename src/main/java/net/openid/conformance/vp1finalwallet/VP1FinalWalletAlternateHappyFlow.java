package net.openid.conformance.vp1finalwallet;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddMismatchedIssToRequestObject;
import net.openid.conformance.condition.client.AddRandomParameterToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddResponseUriToAuthorizationEndpointRequest;
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
		- Includes the optional 'state' parameter, using a longer-than-default value (64 chars)
		- Uses a longer 'nonce' than the default (43 chars)
		- Includes a random authorization endpoint parameter (which must be ignored)
		- Includes an 'iss' claim in the request object that does not match 'client_id' (which must be ignored as per VP spec section 5)
		- Encryption key without 'use: enc' (for encrypted response modes)
		- Reordered query parameters in the redirect URL (no-op for DC API response modes, which don't use a redirect URL)
		- response_uri response returns a redirect_uri which the wallet must open (no-op for DC API response modes; for ISO mdoc the default flow already returns a redirect_uri)
		- response_uri request parameter is omitted when client_id_prefix=redirect_uri and response_mode is direct_post or direct_post.jwt (per OID4VP §5.9.3, the wallet must derive it from client_id)""",
	profile = "OID4VP-1FINAL"
)

public class VP1FinalWalletAlternateHappyFlow extends AbstractVP1FinalWalletTest {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		callAndStopOnFailure(CreateRedirectUri.class);
		// Use the canonical "256-bit" nonce length: base64url(32 random bytes) = 43 characters.
		// This matches the upper bound enforced by CheckNonceMaximumLength on the verifier side.
		env.putInteger("requested_nonce_length", 43);

		// also use a longer state value than is used by default
		env.putInteger("requested_state_length", 64);
	}

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence createAuthorizationRequestSteps = super.createAuthorizationRequestSequence();

		createAuthorizationRequestSteps = createAuthorizationRequestSteps.
			then(condition(AddRandomParameterToAuthorizationEndpointRequest.class));

		createAuthorizationRequestSteps.replace(AddVP1FinalEncryptionParametersToClientMetadata.class,
			condition(AddVP1FinalEncryptionParametersToClientMetadataWithoutUseEnc.class));

		if (clientIdPrefix == VP1FinalWalletClientIdPrefix.REDIRECT_URI
			&& (responseMode == VP1FinalWalletResponseMode.DIRECT_POST
				|| responseMode == VP1FinalWalletResponseMode.DIRECT_POST_JWT)) {
			// OID4VP §5.9.3: under the redirect_uri Client Identifier Prefix, the Verifier MAY omit
			// response_uri when Response Mode `direct_post` is used. direct_post.jwt builds on
			// direct_post and the same carve-out applies. Exercise it here; the wallet must derive
			// the value from the client_id itself.
			createAuthorizationRequestSteps.skip(AddResponseUriToAuthorizationEndpointRequest.class,
				"response_uri is optional under client_id_prefix=redirect_uri + direct_post (OID4VP §5.9.3)");
		}

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
