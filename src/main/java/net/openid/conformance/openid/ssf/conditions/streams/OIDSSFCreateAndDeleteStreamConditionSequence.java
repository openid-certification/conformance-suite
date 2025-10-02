package net.openid.conformance.openid.ssf.conditions.streams;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * Creates and deletes a stream. This will leave the previously set stream_id in the context.
 */
public class OIDSSFCreateAndDeleteStreamConditionSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		// create a stream
		// extract stream_id
		call(sequence(OIDSSFCreateStreamConditionSequence.class));
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.1");

		// delete the stream
		callAndStopOnFailure(OIDSSFDeleteStreamConfigCall.class, "OIDSSF-8.1.1.5", "CAEPIOP-2.3.8.2");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-8.1.1.5");
	}
}
