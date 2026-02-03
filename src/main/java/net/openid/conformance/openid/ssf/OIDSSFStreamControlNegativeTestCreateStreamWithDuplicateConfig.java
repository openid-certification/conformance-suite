package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIsAnyOf;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-create-stream-with-duplicate-config",
	displayName = "Attempt to create multiple stream configurations with same access token.",
	summary = "This test verifies the behavior of the stream control for error cases. It attempts to create multiple stream configurations with the same access token.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-6.2.1
	}
)
public class OIDSSFStreamControlNegativeTestCreateStreamWithDuplicateConfig extends AbstractStreamControlErrorTest {

	@Override
	protected void testTransmitter() {

		// expect 409	if the Transmitter does not support multiple streams per Receiver
		eventLog.runBlock("Attempt to create multiple stream configuration with same access token", () -> {

			call(sequence(OIDSSFCreateStreamConditionSequence.class));

			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
			call(exec().unmapKey("endpoint_response"));

			eventLog.log("Attempt to create another stream definition", args());

			// 409	if authorization failed or it is missing
			call(sequence(OIDSSFCreateStreamConditionSequence.class));

			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(new EnsureHttpStatusCodeIsAnyOf(201, 409) {
				@Override
				protected String createSuccessMessage(String endpointName, int statusCode) {
					String successMessage = super.createSuccessMessage(endpointName, statusCode);
					switch(statusCode) {
						case 201:
							successMessage +=" Transmitter supports multiple streams per Receiver.";
							break;
						case 409:
							successMessage +=" Transmitter does not support multiple streams per Receiver.";
					}
					return successMessage;
				}
			}, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
