package net.openid.conformance.openid.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.as.EnsureResponseTypeIsCode;
import net.openid.conformance.condition.as.SetServerSigningAlgToNone;
import net.openid.conformance.condition.as.SignIdTokenWithAlgNone;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithIdTokenSignedResponseAlgNone;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithIdTokenSignedResponseAlgRS256;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-client-test-idtoken-sig-none",
	displayName = "OIDCC: Relying party test. Use code flow to retrieve an unsigned id_token",
	summary = "The client is expected to accept the unsigned id_token obtained using code flow." +
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
}
