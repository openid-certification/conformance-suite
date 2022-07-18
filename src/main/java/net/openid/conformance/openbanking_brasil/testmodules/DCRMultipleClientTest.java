package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ClientManagementEndpointAndAccessTokenRequired;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalBrazilDCR;
import net.openid.conformance.openbanking_brasil.testmodules.support.CheckScopesFromDynamicRegistrationEndpointContainsConsentsOrPayments;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "dcr-test-multiple-clients",
	displayName = "DCR test: Ensure server accepts multiple clients with the same credentials",
	summary = "Performs DCR against the target server\n" +
		"\u2022 Expect success 201 returned, first client_id created for this set of credentials\n" +
		"\u2022 Perform second DCR against the target server \n" +
		"\u2022 Expect success 201 returned, second client_id created for this set of credentials\n" +
		"\u2022 Unregister both created clients -> This step cannot be skipped even if only one client is created",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase"
	}
)

public class DCRMultipleClientTest extends AbstractFAPI1AdvancedFinalBrazilDCR {
	@Override
	protected void setupResourceEndpoint() {
	}

	@Override
	protected boolean scopeContains(String requiredScope) {
		return false;
	}

	@Override
	protected void callRegistrationEndpoint() {
		eventLog.startBlock("Create First Client");
		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));
		callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");
		call(exec().mapKey("endpoint_response", "dynamic_registration_endpoint_response"));
		callAndStopOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE);
		deleteClient();
		eventLog.endBlock();
		eventLog.startBlock("Create Second Client");
		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));
		callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");
		call(exec().mapKey("endpoint_response", "dynamic_registration_endpoint_response"));
		callAndStopOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE);
		eventLog.endBlock();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		super.onPostAuthorizationFlowComplete();
	}
}
