package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.EnsureResponseTypeIsCode;
import net.openid.conformance.condition.as.SetServerSigningAlgToNone;
import net.openid.conformance.condition.as.SignIdTokenWithAlgNone;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithIdTokenSignedResponseAlgNone;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * As per https://bitbucket.org/openid/connect/issues/1214/certification-remove-requirement-for-rp-to
 * this test should pass with a warning result even if alg none is not supported by the client.
 * If it's not supported we will not receive a userinfo request and the test will transition into
 * finished state after a timeout
 */
@PublishTestModule(
	testName = "oidcc-client-test-idtoken-sig-none",
	displayName = "OIDCC: Relying party test. Use code flow to retrieve an unsigned id_token",
	summary = "The client can either accept the unsigned id_token obtained using code flow and send a userinfo request to " +
		"complete the test or reject the unsigned id_token and stop without sending a userinfo request. " +
		"If a userinfo request is not received then the test will transition into 'skipped'' state after a timeout." +
		" Corresponds to rp-id_token-sig-none test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values = {"code id_token", "code id_token token", "code token", "id_token", "id_token token"})
public class OIDCCClientTestIdTokenSigAlgNone extends AbstractOIDCCClientTest {

	@Override
	protected void setServerSigningAlgorithm() {
		callAndStopOnFailure(SetServerSigningAlgToNone.class);
	}

	@Override
	protected void signIdToken() {
		callAndStopOnFailure(SignIdTokenWithAlgNone.class);
	}

	@Override
	protected void validateResponseTypeAuthorizationRequestParameter() {
		callAndStopOnFailure(EnsureResponseTypeIsCode.class, "OIDCR-2");
	}

	@Override
	protected Class<? extends ConditionSequence> getAdditionalClientRegistrationSteps() {
		return OIDCCRegisterClientWithIdTokenSignedResponseAlgNone.class;
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {
		Object valueFromSuper = super.authorizationCodeGrantType(requestId);
		startWaitingForTimeout();
		return valueFromSuper;
	}

	/**
	 * if a userinfo request is not received, set result to WARNING and finish the test
	 */
	@Override
	protected void startWaitingForTimeout() {
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(waitTimeoutSeconds * 1000L);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				fireTestSkipped("Client did not send a userinfo request after receiving an unsigned id_token. As clients are not required to support unsigned (alg: none) id_tokens this is okay.");
			}
			return "done";
		});
	}
}
