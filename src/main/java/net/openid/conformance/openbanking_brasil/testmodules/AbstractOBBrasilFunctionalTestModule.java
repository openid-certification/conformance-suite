package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = {"openbanking_uk", "plain_fapi", "consumerdataright_au"})
public abstract class AbstractOBBrasilFunctionalTestModule extends AbstractFunctionalTestModule {
}
