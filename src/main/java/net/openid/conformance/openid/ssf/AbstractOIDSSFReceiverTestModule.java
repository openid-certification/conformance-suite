package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.variant.VariantParameters;

@VariantParameters({
	SsfProfile.class,
	SsfDeliveryMode.class,
})
public abstract class AbstractOIDSSFReceiverTestModule extends AbstractOIDSSFTestModule {
}
