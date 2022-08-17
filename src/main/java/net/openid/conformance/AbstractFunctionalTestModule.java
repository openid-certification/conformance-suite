package net.openid.conformance;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalServerTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddOpenIdScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas200;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

// we never need the keystore url, we always override it to point at sandbox keystore
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"directory.keystore"
})
public abstract class AbstractFunctionalTestModule extends AbstractFAPI1AdvancedFinalServerTestModule {

	protected boolean validationStarted = false;

	@Override
	protected void requestProtectedResource() {
		if(!validationStarted) {
			validationStarted = true;
			super.requestProtectedResource();
			eventLog.startBlock(currentClientString() + "Validate response");
			validateResponse();
			eventLog.endBlock();
		}
	}

	protected void preCallProtectedResource(String blockHeader) {

			eventLog.startBlock(currentClientString() + blockHeader);

			preCallProtectedResource();

			eventLog.endBlock();
	}

	protected void preCallProtectedResource() {

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-3");

		callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-5");

		callAndStopOnFailure(CallProtectedResource.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");
		callAndStopOnFailure(EnsureResponseCodeWas200.class);


		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndContinueOnFailure(EnsureResourceResponseReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-9", "FAPI1-BASE-6.2.1-10");

	}

	@Override
	protected void configureClient() {

		callAndStopOnFailure(GetStaticClientConfiguration.class);
		callAndStopOnFailure(AddOpenIdScope.class);

		exposeEnvString("client_id");

		// Test won't pass without MATLS, but we'll try anyway (for now)
		callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);

		validateClientConfiguration();

	}

	@Override
	protected void switchToSecondClient() {

	}

	protected abstract void validateResponse();

	/**
	 * Returns an instance of class ConditionSequenceRepeater
	 * This allows us to repeatedly run a given ConditionSequence until
	 * certain conditions are met, or the repeats time out. Standard timeout is 5 repeats
	 *
	 * @param conditionSequenceSupplier - a Supplier which provides instances of the condition sequence in question
	 *                                    Note: it has to be a supplier, because re-evaluating a condition sequence has
	 *                                    unintended side effects
	 * @return
	 */
	protected ConditionSequenceRepeater repeatSequence(Supplier<ConditionSequence> conditionSequenceSupplier) {
		return new ConditionSequenceRepeater(conditionSequenceSupplier);
	}
}
