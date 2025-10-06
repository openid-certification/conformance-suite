package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;


@VariantParameters({SsfServerMetadata.class, SsfAuthMode.class, SsfDeliveryMode.class,})
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

		eventLog.runBlock("Prepare Transmitter Access", this::obtainTransmitterAccessToken);

		SsfDeliveryMode deliveryMode = getVariant(SsfDeliveryMode.class);
		env.putString("ssf", "delivery_method", deliveryMode.getAlias());
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
