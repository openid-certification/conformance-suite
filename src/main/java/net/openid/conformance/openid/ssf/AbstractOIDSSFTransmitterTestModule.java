package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.conditions.OIDSSFConfigurePushDeliveryMethod;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;

import java.util.Objects;

public class AbstractOIDSSFTransmitterTestModule extends AbstractOIDSSFTestModule {

	@Override
	protected void configureServerEndpoints() {
		super.configureServerEndpoints();

		// When we test a transmitter we need to act as a receiver
		if (Objects.requireNonNull(getVariant(SsfDeliveryMode.class)) == SsfDeliveryMode.PUSH) {
			callAndStopOnFailure(OIDSSFConfigurePushDeliveryMethod.class);
			exposeEnvString("pushDeliveryEndpointUrl", "ssf", "push_delivery_endpoint_url");
		}
	}
}
