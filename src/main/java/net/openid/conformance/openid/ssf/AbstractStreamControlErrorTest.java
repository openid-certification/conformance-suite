package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.openid.ssf.variant.SsfServerMetadata;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ConfigurationFields;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;


@VariantParameters({SsfServerMetadata.class, SsfAuthMode.class, SsfDeliveryMode.class, SsfProfile.class,})
@ConfigurationFields({
	"ssf.transmitter.issuer",
	"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-6.2.1
})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "static", configurationFields = {"ssf.transmitter.configuration_metadata_endpoint",})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "discovery", configurationFields = {"ssf.transmitter.issuer", "ssf.transmitter.metadata_suffix",})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "static", configurationFields = {
	"ssf.transmitter.access_token"
})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "dynamic", configurationFields = {
})
public abstract class AbstractStreamControlErrorTest extends AbstractOIDSSFTransmitterTestModule {

	protected void prepareTransmitterAccess() {
		eventLog.runBlock("Fetch Transmitter Metadata", this::fetchTransmitterMetadata);

		onTransmitterMetadataFetched();

		eventLog.runBlock("Prepare Transmitter Access", this::obtainTransmitterAccessToken);

		env.putString("ssf", "delivery_method", deliveryMode.getAlias());
	}

	/**
	 * Hook invoked once the transmitter metadata is available and before an access token is
	 * obtained. Modules that exercise an endpoint the SSF specification makes optional call
	 * {@link #requireStatusEndpoint()} or {@link #requireVerificationEndpoint()} here, so a
	 * transmitter without that endpoint is skipped (or failed under the CAEP Interop Profile)
	 * before any token or stream is created.
	 */
	protected void onTransmitterMetadataFetched() {
		// NOOP
	}

	/**
	 * Skips the test if the transmitter metadata has no {@code status_endpoint}. SSF 1.0 (7.1)
	 * makes the field OPTIONAL; the CAEP Interop Profile (2.3.5) requires it, so under that
	 * profile the missing endpoint is a failure.
	 */
	protected void requireStatusEndpoint() {
		if (env.getString("ssf", "transmitter_metadata.status_endpoint") != null) {
			return;
		}
		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			throw new TestFailureException(getId(), "Transmitter metadata does not include a status_endpoint, "
				+ "which is required by the CAEP Interop Profile (CAEPIOP-2.3.5).");
		}
		fireTestSkipped("Transmitter metadata does not include a status_endpoint. "
			+ "SSF 1.0 section 7.1 defines status_endpoint as \"OPTIONAL. The URL of the Status Endpoint.\" (OIDSSF-7.1).");
	}

	/**
	 * Skips the test if the transmitter metadata has no {@code verification_endpoint}. SSF 1.0
	 * (7.1) makes the field OPTIONAL; the CAEP Interop Profile (2.3.6) requires it, so under
	 * that profile the missing endpoint is a failure.
	 */
	protected void requireVerificationEndpoint() {
		if (env.getString("ssf", "transmitter_metadata.verification_endpoint") != null) {
			return;
		}
		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			throw new TestFailureException(getId(), "Transmitter metadata does not include a verification_endpoint, "
				+ "which is required by the CAEP Interop Profile (CAEPIOP-2.3.6).");
		}
		fireTestSkipped("Transmitter metadata does not include a verification_endpoint. "
			+ "SSF 1.0 section 7.1 defines verification_endpoint as \"OPTIONAL. The URL of the Verification Endpoint.\" (OIDSSF-7.1).");
	}

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		prepareTransmitterAccess();
		beforeTestTransmitter();

		testTransmitter();

		fireTestFinished();
	}

	protected void beforeTestTransmitter() {
		eventLog.runBlock("Clean stream environment if necessary", this::cleanUpStreamConfigurationIfNecessary);
	}

	/**
	 * Implement the actual transmitter test logic here
	 */
	protected abstract void testTransmitter();
}
