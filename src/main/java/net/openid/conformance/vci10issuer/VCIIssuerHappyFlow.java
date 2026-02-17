package net.openid.conformance.vci10issuer;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.EnsureIdTokenDoesNotContainNonRequestedClaims;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import net.openid.conformance.condition.common.CheckForBCP195InsecureFAPICiphers;
import net.openid.conformance.condition.common.DisallowInsecureCipher;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12WithFAPICiphers;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-happy-flow",
	displayName = "OID4VCI 1.0: Issuer happy flow",
	summary = "This test case validates the standard credential issuance flow using an emulated wallet, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification. It begins by retrieving metadata from both the Credential Issuer and the OAuth 2.0 Authorization Server. An authorization request is initiated using Pushed Authorization Requests (PAR), and an access token is obtained. The test then retrieves a nonce from the Credential Endpoint, constructs a DPoP proof JWT bound to the nonce, and successfully requests a credential from the Credential Endpoint.",
	profile = "OID4VCI-1_0",
	configurationFields = {
		"vci.credential_issuer_url",
		"client.client_id",
		"client.jwks",
		"client2.client_id",
		"client2.jwks",
		"vci.credential_configuration_id",
		"vci.credential_proof_type_hint",
		"vci.key_attestation_jwks",
		"vci.authorization_server",
	}
)
public class VCIIssuerHappyFlow extends AbstractVCIIssuerMultipleClient {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		if (isOpenId) {
			Command cmd = new Command();

			if (isSecondClient()) {
				cmd.putInteger("requested_nonce_length", 43);
			} else {
				cmd.removeNativeValue("requested_nonce_length");
			}

			ConditionSequence conditionSequence = super.makeCreateAuthorizationRequestSteps()
				.insertBefore(CreateRandomNonceValue.class, cmd);

			return conditionSequence;
		}

		return super.makeCreateAuthorizationRequestSteps();
	}

	protected void checkResourceEndpointTLS() {
		eventLog.startBlock("Resource endpoint TLS test");
		env.mapKey("tls", "resource_endpoint_tls");
		checkEndpointTLS();
		env.unmapKey("tls");
		eventLog.endBlock();
	}

	protected void checkEndpointTLS() {
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.3-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.1-1");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.1-1");
		callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.2.1");
		callAndContinueOnFailure(CheckForBCP195InsecureFAPICiphers.class, Condition.ConditionResult.WARNING, "FAPI1-ADV-8.5", "RFC9325A-A", "RFC9325-4.2");
	}

	@Override
	protected void performAuthorizationFlowWithSecondClient() {
		// NOOP
	}

	protected void performAdditionalResourceEndpointTests() {
		// NOOP
	}

	@Override
	protected void requestProtectedResource() {

		if (!isSecondClient()) {
			callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);
			checkResourceEndpointTLS();
		}

		super.requestProtectedResource();

		if (!isSecondClient()) {
			performAdditionalResourceEndpointTests();
		}

		fireTestFinished();
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {

		if (isOpenId && !isSecondClient()) {
			callAndContinueOnFailure(EnsureIdTokenDoesNotContainNonRequestedClaims.class, Condition.ConditionResult.WARNING);
		}

		super.onPostAuthorizationFlowComplete();
	}
}
