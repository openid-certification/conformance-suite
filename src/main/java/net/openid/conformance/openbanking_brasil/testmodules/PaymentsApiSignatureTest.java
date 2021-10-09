

package net.openid.conformance.openbanking_brasil.testmodules;

	import net.openid.conformance.condition.client.*;
	import net.openid.conformance.openbanking_brasil.OBBProfile;
	import net.openid.conformance.openbanking_brasil.testmodules.support.*;
	import net.openid.conformance.sequence.ConditionSequence;
	import net.openid.conformance.testmodule.PublishTestModule;

	import java.util.Random;

@PublishTestModule(
	testName = "payments-consents-bad-signature-force-test",
	displayName = "Payments Consents API basic test module ",
	summary = "Payments Consents API basic test module",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)
public class PaymentsApiSignatureTest extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainPaymentsAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	protected void runTests() {

		int numOfGoodTests = new Random().nextInt(20) + 1;

		eventLog.startBlock("Making a random number of good payment consent requests");
		for(int i = 0; i < numOfGoodTests; i++){
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);
			call(sequence(SignedPaymentConsentSequence.class));

		}

		eventLog.startBlock("Trying a badly signed payment consent request");
		call(new SignedPaymentConsentSequence()
			.replace(EnsureHttpStatusCodeIs201.class, condition(EnsureHttpStatusCodeIs400.class))
			.insertAfter(FAPIBrazilSignPaymentConsentRequest.class, condition(InvalidateConsentEndpointRequestSignature.class))
		);

	}
}