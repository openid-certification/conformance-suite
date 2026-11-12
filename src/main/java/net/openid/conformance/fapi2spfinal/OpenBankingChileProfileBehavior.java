package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.function.Supplier;

/**
 * Profile behavior for Chile Open Finance.
 * Requires mTLS everywhere, and sends a new interaction id on every resource call.
 */
public class OpenBankingChileProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	/**
	 * Every resource request carries a new x-fapi-interaction-id, for both clients
	 * (CMF-XJWS-5.2.1-6). The CDR auth-date and customer IP headers are not part of the profile.
	 */
	@Override
	public ConditionSequence addResourceEndpointProfileHeaders(boolean isSecondClient) {
		return newInteractionId();
	}

	@Override
	public ConditionSequence addAlternateResourceEndpointProfileHeaders() {
		return null;
	}

	/** A retried or repeated request gets a new interaction id (CMF-XJWS-5.2.1-6). */
	@Override
	public ConditionSequence createUpdateResourceRequestSteps(
			Supplier<? extends ConditionSequence> createDpopForResourceEndpointSteps) {
		ConditionSequence base = super.createUpdateResourceRequestSteps(createDpopForResourceEndpointSteps);
		ConditionSequence interactionId = newInteractionId();
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(base);
				call(interactionId);
			}
		};
	}

	private ConditionSequence newInteractionId() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
				callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "CMF-XJWS-5.2.1-6");
			}
		};
	}
}
