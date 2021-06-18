package net.openid.conformance.fapi1advancedfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.as.EnsureRequestObjectWasEncrypted;
import net.openid.conformance.condition.as.FAPIBrazilEnsureRequestObjectEncryptedUsingRSAOAEPA256GCM;
import net.openid.conformance.condition.as.LogAccessTokenAlwaysRejectedToForceARefreshGrant;
import net.openid.conformance.condition.as.RemoveIssuedAccessTokenFromEnvironment;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-encrypted-requestobject-test",
	displayName = "FAPI1-Advanced-Final: client encrypted request object support test",
	summary = "Tests the happy path flow but requires request object encryption, regardless of whether PAR is used or not." +
		" Server jwks configured in test configuration MUST contain a key usable for encryption for this test to succeed.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = {"plain_fapi", "consumerdataright_au", "openbanking_uk"})
//TODO re-evaluate if this test is necessary or how this could be implemented better.
// when not using PAR these checks will be always applied at the authorization endpoint so a separate test is not
// needed for this. But when using PAR, we need a separate test to test Brazil "5.2.3-1 shall support encrypted request objects"
// but as per "5.2.3-2 shall support Pushed Authorisation Requests PAR" a client must always support PAR and also encrypted
// request objects.
public class FAPI1AdvancedFinalClientEncryptedRequestObjectTest extends AbstractFAPI1AdvancedFinalClientTest {

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected void endTestIfRequiredParametersAreMissing() {
		callAndStopOnFailure(EnsureRequestObjectWasEncrypted.class, "BrazilOB-5.2.3-1");
		callAndStopOnFailure(FAPIBrazilEnsureRequestObjectEncryptedUsingRSAOAEPA256GCM.class, "BrazilOB-6.1.1-1");
	}
}
