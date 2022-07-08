package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildCreditOperationsAdvancesConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildResourcesConfigResourceUrlFromConsentUrl;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "resources-api-dcr-subjectdn",
	displayName = "Resources API test that DCR works with both numeric and string oids",
	summary = "Obtain a software statement from the Brazil sandbox directory (using a hardcoded client that has the DADOS role), register a new client on the target authorization server and try the client credentials grant. This is done twice - one where the Brazil specific OIDs are in numeric form (which must be accepted), and one with them in the string form (which should be accepted). Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl"
	}
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = { "private_key_jwt" }) // only applicable for mtls client auth
// hide various config values from the FAPI base module we don't need
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks",
	"resource.brazilOrganizationId",
	"resource.brazilPaymentConsent",
	"resource.brazilPixPayment",
})
public class ResourcesApiDcrSubjectDn extends AbstractApiDcrSubjectDn {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildResourcesConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected boolean isPaymentsApiTest() {
		return false;
	}

}
