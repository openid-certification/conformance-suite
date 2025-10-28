package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

@PublishTestModule(
		testName = "openid-federation-automatic-client-registration-with-jar-and-encrypted-request-object",
		displayName = "openid-federation-automatic-client-registration-with-jar-and-encrypted-request-object",
		summary = "The test acts as an RP wanting to perform automatic client registration with an OP, " +
			"with JAR and HTTP GET to the authorization endpoint. The request object is encrypted using the OP's " +
			"published, public encryption key.",
		profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationWithJarAndEncryptedRequestObjectTest extends OpenIDFederationAutomaticClientRegistrationTest {

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return FAPIAuthRequestMethod.BY_VALUE;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return HttpMethod.GET;
	}

	@Override
	protected void verifyTestConditions() {
		JsonElement encAlgValuesSupported = env.getElementFromObject("primary_entity_statement_jwt", "claims.metadata.openid_provider.request_object_encryption_alg_values_supported");
		JsonElement encEncValuesSupported = env.getElementFromObject("primary_entity_statement_jwt", "claims.metadata.openid_provider.request_object_encryption_enc_values_supported");
		if (encAlgValuesSupported == null || encEncValuesSupported == null) {
			fireTestSkipped("The server does not support encrypted request objects");
		}
	}

	@Override
	protected void encryptRequestObject() {
		callAndContinueOnFailure(EncryptRequestObject.class, Condition.ConditionResult.FAILURE);
	}

}
