package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalBrazilDCR;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

// hide various config values from superclasses we never need in functional as we're always using sandbox directory etc
// There's no "this test module doesn't need a configuration value from the parent class, so we use the existing
// VariantHidesConfigurationFields.
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"directory.keystore"
})
public abstract class AbstractApiDcrTestModule extends AbstractFAPI1AdvancedFinalBrazilDCR {
}
