package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
		testName = "openid-federation-automatic-client-registration",
		displayName = "openid-federation-automatic-client-registration",
		summary = "The test acts as an RP wanting to perform automatic client registration with an OP",
		profile = "OIDFED",
		configurationFields = {
			"client.jwks",
			"federation.entity_identifier",
			"federation.trust_anchor_jwks"
		}
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationTest extends AbstractOpenIDFederationTest {

	@Override
	public void additionalConfiguration() {
		eventLog.startBlock("Additional configuration");
		JsonObject clientConfig = env.getElementFromObject("config", "client").getAsJsonObject();
		clientConfig.addProperty("client_id", env.getString("base_url"));

		callAndStopOnFailure(GetStaticClientConfiguration.class);
		callAndStopOnFailure(ValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		eventLog.endBlock();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		callAndContinueOnFailure(CreateRequestObjectClaims.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(SignRequestObject.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CallAuthorizationEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE);
		Environment _env = env;

		setStatus(Status.WAITING);
		//fireTestFinished();
	}

}
