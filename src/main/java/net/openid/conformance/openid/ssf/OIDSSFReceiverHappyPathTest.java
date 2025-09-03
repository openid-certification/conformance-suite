package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;

@PublishTestModule(
	testName = "openid-ssf-receiver-happypath",
	displayName = "OpenID Shared Signals Framework: Validate Receiver Handling",
	summary = "This test verifies the receiver stream management and event delivery. " +
		"The test generates a dynamic transmitter and waits for a receiver to register a stream. " +
		"Then the testsuite will generate some supported events and deliver it to the receiver via the configured delivery mechanism. " +
		"The testsuite will then wait for acknowledgement of those events.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.access_token",
		"ssf.stream.audience",
		"ssf.subjects.valid",
		"ssf.subjects.invalid"
	}
)
@VariantConfigurationFields(
	parameter = SsfDeliveryMode.class,
	value="push",
	configurationFields = {"ssf.transmitter.push_endpoint_authorization_header"}
)
public class OIDSSFReceiverHappyPathTest extends AbstractOIDSSFReceiverTestModule {

	@Override
	protected void afterStreamCreation(JsonObject createResult) {
		super.afterStreamCreation(createResult);

		// if cape interop profile
		// enqueue supported cape events
	}
}
