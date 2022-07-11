package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "payments-api-dcr-subjectdn",
	displayName = "Payments API test that DCR works with both numeric and string oids",
	summary = "Obtain a software statement from the Brazil sandbox directory (using a hardcoded client that has the PAGTO role), register a new client on the target authorization server and try the client credentials grant. This is done twice - one where the Brazil specific OIDs are in numeric form (which must be accepted), and one with them in the string form (which should be accepted). Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl"
	}
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = { "private_key_jwt" }) // only applicable for mtls client auth
public class PaymentsApiDcrSubjectDn extends AbstractApiDcrSubjectDn {

	@Override
	protected boolean isPaymentsApiTest() {
		return true;
	}

}
