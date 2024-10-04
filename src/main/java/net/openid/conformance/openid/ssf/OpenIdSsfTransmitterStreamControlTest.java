package net.openid.conformance.openid.ssf;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-transmitter-stream-control",
	displayName = "OpenID Shared Signals Framework: Validate Transmitter Stream Control",
	summary = "This test verifies the behavior of the transmitter stream control.",
	profile = "OIDSSF",
	configurationFields = {
	}
)
public class OpenIdSsfTransmitterStreamControlTest extends AbstractOpenIdSsfTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		// see https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html
		// OID_CAEP_INTEROP https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html

		//OID_CAEP_INTEROP-2.3.8.2 Stream control (except Status) - check for API availability
		// Creating a Stream
		// Reading Stream Configuration
		// Getting the Stream Status
		// Stream Verification

		fireTestFinished();
	}

}
