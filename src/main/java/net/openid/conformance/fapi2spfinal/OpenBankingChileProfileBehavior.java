package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureChileResponseSigningJwksUriConfigured;
import net.openid.conformance.condition.client.SignResourceRequestWithChileXJwsSignature;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.function.Supplier;

/**
 * Profile behavior for Chile Open Finance.
 * Requires mTLS everywhere, and signs every resource request with the CMF x-jws-signature
 * (CMF-XJWS-5.2.1), with a new interaction id and signature on every call.
 */
public class OpenBankingChileProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public ConditionSequence setupResourceEndpoint() {
		ConditionSequence base = super.setupResourceEndpoint();
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(base);
				callAndStopOnFailure(EnsureChileResponseSigningJwksUriConfigured.class, "CMF-XJWS-5.3.2-1");
			}
		};
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

	@Override
	public ConditionSequence setupResourceEndpointRequestBody() {
		return signRequest();
	}

	/** A retried or repeated request gets a new interaction id and a new signature (CMF-XJWS-5.2.1-6, -7). */
	@Override
	public ConditionSequence createUpdateResourceRequestSteps(
			Supplier<? extends ConditionSequence> createDpopForResourceEndpointSteps) {
		ConditionSequence base = super.createUpdateResourceRequestSteps(createDpopForResourceEndpointSteps);
		ConditionSequence interactionId = newInteractionId();
		ConditionSequence signature = signRequest();
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(base);
				call(interactionId);
				call(signature);
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

	private ConditionSequence signRequest() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(SignResourceRequestWithChileXJwsSignature.class,
					"CMF-XJWS-5.2.1-1", "CMF-XJWS-5.2.1-2", "CMF-XJWS-5.2.1-3", "CMF-XJWS-5.2.1-4",
					"CMF-XJWS-5.2.1-5", "CMF-XJWS-5.2.1-7");
			}
		};
	}
}
