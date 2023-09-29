package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import net.openid.conformance.condition.client.CheckForSubjectInIdToken;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.EnsureIdTokenContainsKid;
import net.openid.conformance.condition.client.ExtractMTLSCertificates2FromConfiguration;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateIdToken;
import net.openid.conformance.condition.client.ValidateIdTokenNonce;
import net.openid.conformance.condition.client.ValidateIdTokenSignature;
import net.openid.conformance.condition.client.ValidateIdTokenStandardClaims;
import net.openid.conformance.condition.client.ValidateMTLSCertificates2Header;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-ensure-authorization-code-is-bound-to-client",
	displayName = "FAPI1-Advanced-Final: ensure authorization code is bound to client",
	summary = "This test ensures the token endpoint returns an error if a valid authorization code is used with another client's credentials.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPI1AdvancedFinalEnsureAuthorizationCodeIsBoundToClient extends AbstractFAPI1AdvancedFinalServerTestModule {

	@Override
	protected void performIdTokenValidation() {

		// This hasn't been changed to use PerformStandardIdTokenChecks as it's unclear what checks it's trying to do
		// differently to super.
		callAndContinueOnFailure(ValidateIdToken.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-4");
		callAndContinueOnFailure(ValidateIdTokenStandardClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-5.1");

		callAndContinueOnFailure(EnsureIdTokenContainsKid.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");

		callAndContinueOnFailure(ValidateIdTokenNonce.class, Condition.ConditionResult.FAILURE,"OIDCC-2");

		callAndContinueOnFailure(ValidateIdTokenSignature.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-4");

		callAndStopOnFailure(CheckForSubjectInIdToken.class, "FAPI1-BASE-5.2.2.1-6", "OB-5.2.2-8");
	}

	@Override
	protected void performPostAuthorizationFlow() {

		createAuthorizationCodeRequest();

		// Now try with the wrong certificate
		callAndContinueOnFailure(ValidateMTLSCertificates2Header.class, Condition.ConditionResult.WARNING);
		callAndStopOnFailure(ExtractMTLSCertificates2FromConfiguration.class);

		switchToSecondClient();

		createAuthorizationCodeRequest();

		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-6");
		callAndStopOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");

		unmapClient();

		fireTestFinished();
	}
}
