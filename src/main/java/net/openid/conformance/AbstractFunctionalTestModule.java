package net.openid.conformance;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalServerTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddOpenIdScope;
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

		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");

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
	 * Returns an instance of inner class ConditionSequenceRepeater
	 * This allows us to reoeatedly run a given ConditionSequence until
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

	protected class ConditionSequenceRepeater {

		private final Logger logger = LoggerFactory.getLogger(ConditionSequenceRepeater.class);

		private final Supplier<ConditionSequence> sequenceSupplier;
		private final String TIMEOUT_COUNTER_KEY = String.format("LOOPING_COUNTER_%s", getId());
		private int timeout = 5;
		private Optional<Class<? extends Condition>> onTimeoutCondtion = Optional.empty();
		private Optional<Class<? extends Condition>> preSequencePause = Optional.empty();
		private Optional<Class<? extends Condition>> postSequencePause = Optional.of(WaitForConfiguredSeconds.class);
		private Optional<ConditionSequence> onTimeoutCondtionSequence = Optional.empty();

		private Predicate<Environment> timeoutPredicate = (e) -> {
			int ttl = e.getInteger(TIMEOUT_COUNTER_KEY);
			logger.info("TTL on repeating condition sequence for {} now {}", getName(), ttl);
			return !(ttl < timeout);
		};

		private Predicate<Environment> endPredicate = timeoutPredicate;

		public ConditionSequenceRepeater(Supplier<ConditionSequence> conditionSequenceSupplier) {
			this.sequenceSupplier = conditionSequenceSupplier;
			env.putInteger("loopSequencePauseTime", 0);
			env.putInteger("preSequencePauseTime", 0);
			env.putInteger("postSequencePauseTime", 1);
		}

		/**
		 * Call after configuration methods. This actually
		 * starts to run the conditionsequence
		 */
		public void run() {
			int ttl = 1;
			do {
				int pause = env.getInteger("preSequencePauseTime");
				env.putInteger("loopSequencePauseTime", pause);
				preSequencePause.ifPresent(c -> callAndStopOnFailure(c));
				env.putInteger(TIMEOUT_COUNTER_KEY, ttl);
				ConditionSequence sequence = sequenceSupplier.get();
				call(sequence);
				ttl++;
				pause = env.getInteger("postSequencePauseTime");
				env.putInteger("loopSequencePauseTime", pause);
				postSequencePause.ifPresent(c -> callAndStopOnFailure(c));
			} while(!endPredicate.test(env));
			if(timeoutPredicate.test(env)) {
				onTimeoutCondtion.ifPresent(c -> callAndStopOnFailure(c));
				onTimeoutCondtionSequence.ifPresent(s -> call(s));
			}
		}

		/**
		 * Overrides the number of times the sequence is repeated if no other condition ends it
		 * @param timeout
		 * @return
		 */
		public ConditionSequenceRepeater times(int timeout) {
			this.timeout = timeout;
			return this;
		}

		/**
		 * Specifies that there should be a *pause* second pause
		 * before each run of the condition sequence
		 * @param pause
		 * @return
		 */
		public ConditionSequenceRepeater leadingPause(int pause) {
			env.putInteger("preSequencePauseTime", pause);
			return this;
		}

		/**
		 * Specifies that there should be a *pause* second pause
		 * after each run of the condition sequence
		 * @param pause
		 * @return
		 */
		public ConditionSequenceRepeater trailingPause(int pause) {
			env.putInteger("postSequencePauseTime", pause);
			return this;
		}

		/**
		 * We can run the sequence repeatedly until a given boolean value in
		 * the Environment is changed to false. Typically by a condition within the sequence
		 *
		 * @param conditionKey - the name of the boolean value
		 * @return
		 */
		public ConditionSequenceRepeater untilFalse(String conditionKey) {
			env.putBoolean(conditionKey, true);
			return until((e) -> !e.getBoolean(conditionKey));
		}

		/**
		 * We can run the sequence repeatedly until a given boolean value in
		 * the Environment is changed to true. Typically by a condition within the sequence
		 *
		 * @param conditionKey - the name of the boolean value
		 * @return
		 */
		public ConditionSequenceRepeater untilTrue(String conditionKey) {
			env.putBoolean(conditionKey, false);
			return until((e) -> e.getBoolean(conditionKey));
		}

		/**
		 * We can run the sequence repeatedly until a given string value in
		 * the Environment is changed to the given value. Typically by a condition within the sequence
		 *
		 * @param key - the name of the string value
		 * @param value - the expected value of the string which breaks us out of the loop
		 * @return
		 */
		public ConditionSequenceRepeater untilEqual(String key, String value) {
			return until((e) -> env.getString(key).equals(value));
		}

		/**
		 * We can run the sequence repeatedly until a given int value in
		 * the Environment is changed to the given value. Typically by a condition within the sequence
		 *
		 * @param key - the name of the string value
		 * @param value - the expected value of the int which breaks us out of the loop
		 * @return
		 */
		public ConditionSequenceRepeater untilEqual(String key, int value) {
			return until((e) -> env.getInteger(key) == value);
		}

		/**
		 * Generic method for adding additional predicates which may not be otherwise
		 * covered
		 * @param until - a Predicate<Environment> which, when evaluated to true, ends the loop
		 * @return
		 */
		public ConditionSequenceRepeater until(Predicate<Environment> until) {
			endPredicate = endPredicate.or(until);
			return this;
		}

		/**
		 * A
		 * @param conditionClass to be executed if the loop times out
		 * @return
		 */
		public ConditionSequenceRepeater onTimeout(Class<? extends Condition> conditionClass) {
			this.onTimeoutCondtion = Optional.ofNullable(conditionClass);
			return this;
		}

		/**
		 * A
		 * @param endSequence to be executed if the loop times out
		 * @return
		 */
		public ConditionSequenceRepeater onTimeout(ConditionSequence endSequence) {
			this.onTimeoutCondtionSequence = Optional.ofNullable(endSequence);
			return this;
		}





	}

}
