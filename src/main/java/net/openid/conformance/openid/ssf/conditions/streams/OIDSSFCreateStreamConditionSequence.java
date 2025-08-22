package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDSSFCreateStreamConditionSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(OIDSSFPrepareStreamConfigObject.class, "OIDSSF-8.1.1.1");
		callAndStopOnFailure(OIDSSFPrepareStreamConfigObjectAddRequestedEvents.class, "OIDSSF-8.1.1.1");
		callAndContinueOnFailure(OIDSSFPrepareStreamConfigObjectSetDeliveryMethod.class, "OIDSSF-8.1.1.1");
		callAndStopOnFailure(OIDSSFCreateStreamConfigCall.class, "OIDSSF-8.1.1.1", "CAEPIOP-2.3.8.2");
	}
}
