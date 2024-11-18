package net.openid.conformance.openid.ssf;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204;
import net.openid.conformance.condition.client.WaitForOneSecond;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationAuthorizationHeader;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationEventSubjectId;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractVerificationEventFromPushRequest;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFParseVerificationEventToken;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFTriggerVerificationEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationEventState;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfServerMetadata;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

@PublishTestModule(testName = "openid-ssf-transmitter-events", displayName = "OpenID Shared Signals Framework: Validate Transmitter Events", summary = "This test verifies the structure and handling of transmitter events.", profile = "OIDSSF", configurationFields = {

})
@VariantParameters({SsfServerMetadata.class, SsfAuthMode.class, SsfDeliveryMode.class,})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "static", configurationFields = {"ssf.transmitter.configuration_metadata_endpoint",})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "discovery", configurationFields = {"ssf.transmitter.issuer", "ssf.transmitter.metadata_suffix",})
@VariantConfigurationFields(parameter = SsfDeliveryMode.class, value = "push", configurationFields = {"ssf.transmitter.push_endpoint", "ssf.transmitter.push_endpoint_authorization_header"})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "static", configurationFields = {"ssf.transmitter.access_token"})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "dynamic", configurationFields = {})
public class OIDSSFTransmitterEventsTest extends AbstractOIDSSFTest {

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Transmitter Metadata", () -> {
			fetchTransmitterMetadata();
		});

		eventLog.runBlock("Validate TLS Connection", () -> {
			validateTlsConnection();
		});

		eventLog.runBlock("Prepare Transmitter Access", () -> {
			obtainTransmitterAccessToken();
		});

//		try {
//			callAndContinueOnFailure(OIDSSFReadStreamConfigCall.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.3.8.2");
//		} catch (Exception ignore) {
//		}
//		try {
//			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.3.8.2");
//			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
//			callAndContinueOnFailure(EnsureHttpStatusCodeIs204.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.5");
//		} catch (Exception ignore) {
//		}

		SsfDeliveryMode deliveryMode = getVariant(SsfDeliveryMode.class);


		// ensure stream exists

		eventLog.runBlock("Create Stream Configuration", () -> {

			JsonObject deliveryObject = new JsonObject();
			deliveryObject.addProperty("delivery_method", deliveryMode.getAlias());

			JsonElement authorizationHeaderEl = env.getElementFromObject("config", "ssf.transmitter.push_endpoint_authorization_header");
			if (authorizationHeaderEl != null) {
				String pushAuthorizationHeader = OIDFJSON.getString(authorizationHeaderEl);
				if (StringUtils.hasText(pushAuthorizationHeader)) {
					deliveryObject.addProperty("authorization_header", pushAuthorizationHeader);
				}
			}

			env.putObject("ssf", "delivery", deliveryObject);

			callAndStopOnFailure(OIDSSFCreateStreamConfigCall.class, "OIDSSF-7.1.1.1", "CAEPIOP-2.3.8.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
			call(exec().unmapKey("endpoint_response"));
		});

		eventLog.runBlock("Trigger verification event", () -> {
			// Send verification event
			//
			callAndStopOnFailure(OIDSSFTriggerVerificationEvent.class, "OIDSSF-7.1.4.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-7.1.4.2");

//			setStatus(Status.WAITING);
		});

		eventLog.runBlock("Verify verification event", () -> {

			callAndContinueOnFailure(WaitForOneSecond.class, Condition.ConditionResult.INFO);

			switch (deliveryMode) {
				case POLL: {
					// TODO add support for polling delivery
				}
				break;
				case PUSH: {
					// TODO support to read push delivery
					// wait for data received on dynamic endpoint (needs to be reachable externally!)
					callAndStopOnFailure(OIDSSFExtractVerificationEventFromPushRequest.class, "OIDSSF-7.1.4.1");
				}
				break;
			}

			callAndStopOnFailure(OIDSSFParseVerificationEventToken.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.4.1");
			callAndContinueOnFailure(OIDSSFCheckVerificationEventState.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.4.1");
			callAndContinueOnFailure(OIDSSFCheckVerificationEventSubjectId.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.4.1");
			callAndContinueOnFailure(OIDSSFCheckVerificationAuthorizationHeader.class, Condition.ConditionResult.FAILURE,"OIDSSF-10.3.1.1");

//			setStatus(Status.RUNNING);
		});

		fireTestFinished();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if ("ssf-push".equals(path)) {
			env.putObject("ssf", "push_request", requestParts);
			return ResponseEntity.noContent().build();
		}

		return super.handleHttp(path, req, res, session, requestParts);
	}

	@Override
	public void cleanup() {
		eventLog.runBlock("Cleanup", () -> {
			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO);
			super.cleanup();
		});
	}
}
