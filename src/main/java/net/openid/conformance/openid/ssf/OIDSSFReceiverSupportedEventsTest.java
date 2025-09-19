package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamSET;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-stream-supported-events",
	displayName = "OpenID Shared Signals Framework: Sends all supported events to the receiver",
	summary = "This test verifies the receiver stream management. " +
		"The test generates a dynamic transmitter and waits for a receiver to register a stream. ",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.access_token",
		"ssf.stream.audience",
		"ssf.subjects.valid",
		"ssf.subjects.invalid"
	})
@VariantConfigurationFields(
	parameter = SsfDeliveryMode.class,
	value = "push",
	configurationFields = {
		"ssf.transmitter.push_endpoint_authorization_header"
	})
public class OIDSSFReceiverSupportedEventsTest extends AbstractOIDSSFReceiverTestModule {

	volatile String createdStreamId;

	volatile ConcurrentMap<String, Set<String>> eventsAcked;

	volatile ConcurrentMap<String, Set<String>> eventsEnqueued;

	@Override
	public void start() {
		super.start();
		eventsAcked = new ConcurrentHashMap<>();
		eventsEnqueued = new ConcurrentHashMap<>();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 10, TimeUnit.SECONDS);
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		eventsAcked.computeIfAbsent(streamId, k -> new ConcurrentSkipListSet<>()).add(jti);
	}

	@Override
	protected void onStreamEventEnqueued(String streamId, String jti) {
		eventsEnqueued.computeIfAbsent(streamId, k -> new ConcurrentSkipListSet<>()).add(jti);
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {

		if (createResult == null) {
			return;
		}

		createdStreamId = streamId;

		if (createdStreamId != null) {
			scheduleTask(() -> {

				JsonObject validSubject = env.getElementFromObject("config", "ssf.subjects.valid").getAsJsonObject();
				List<String> eventsDelivered = OIDFJSON.convertJsonArrayToList(createResult.getAsJsonObject("result").getAsJsonArray("events_delivered"));

				long timestamp = Instant.now().getEpochSecond();

				for (String eventType : eventsDelivered) {

					SsfEvent event = generateSsfEvent(eventType, timestamp);

					var generateStreamSET = new OIDSSFGenerateStreamSET(eventStore, createdStreamId, validSubject, event, this::onStreamEventEnqueued);
					callAndContinueOnFailure(generateStreamSET, Condition.ConditionResult.WARNING, event.requirements().toArray(new String[0]));
				}

				return "done";
			}, 1, TimeUnit.SECONDS);
		}
	}

	protected SsfEvent generateSsfEvent(String eventType, long timestamp) {
		return switch (eventType) {

			// Examples from CAEP spec below: https://openid.net/specs/openid-caep-1_0-final.html

			case SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp,
					"initiating_entity", "policy",
					"reason_admin", Map.of("en", "Policy Violation: C076E822"),
					"reason_user", Map.of("en", "This device is no longer compliant.", "it", "Questo dispositivo non e piu conforme."))
				, Set.of("OIDCAEP-3.1", "CAEPIOP-3.1"));

			case SsfEvents.CAEP_TOKEN_CLAIMS_CHANGE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp, "claims", Map.of("role", "ro-admin"))
				, Set.of("OIDCAEP-3.2"));

			case SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timest a mp", timestamp,
					"credential_type", "fido2-roaming",
					"change_type", "create",
					"fido2_aaguid", "accced6a-63f5-490a-9eea-e59bc1896cfc",
					"friendly_name", "Jane's USB authenticator",
					"initiating_entity", "user",
					"reason_admin", Map.of("en", "User self-enrollment"))
				, Set.of("OIDCAEP-3.3", "CAEPIOP-3.2"));

			case SsfEvents.CAEP_ASSURANCE_LEVEL_CHANGE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp,
					"namespace", "NIST-AAL",
					"current_level", "nist-aal2",
					"previous_level", "nist-aal1",
					"change_direction", "increase",
					"initiating_entity", "user")
				, Set.of("OIDCAEP-3.4"));

			case SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp,
					"current_status", "not-compliant",
					"previous_status", "compliant",
					"initiating_entity", "policy",
					"reason_admin", Map.of("en", "Location Policy Violation: C076E8A3"),
					"reason_user", Map.of("en", "Device is no longer in a trusted location."))
				, Set.of("OIDCAEP-3.5")
			);

			case SsfEvents.CAEP_SESSION_ESTABLISHED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp,
					"fp_ua", "abb0b6e7da81a42233f8f2b1a8ddb1b9a4c81611",
					"acr", "AAL2",
					"amr", List.of("otp"))
				, Set.of("OIDCAEP-3.6"));

			case SsfEvents.CAEP_SESSION_PRESENTED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp,
					"fp_ua", "abb0b6e7da81a42233f8f2b1a8ddb1b9a4c81611",
					"ext_id", "12345")
				, Set.of("OIDCAEP-3.7"));

			case SsfEvents.CAEP_RISK_LEVEL_CHANGE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("event_timestamp", timestamp,
					"current_level", "LOW",
					"previous_level", "HIGH",
					"initiating_entity", "user",
					"principal", "USER",
					"risk_reason", "PASSWORD_FOUND_IN_DATA_BREACH")
				, Set.of("OIDCAEP-3.8"));

			// Examples from RISC spec below: https://openid.net/specs/openid-risc-1_0-final.html

			case SsfEvents.RISC_ACCOUNT_CREDENTIAL_CHANGE_REQUIRED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.1"));

			case SsfEvents.RISC_ACCOUNT_PURGED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.2"));

			case SsfEvents.RISC_ACCOUNT_DISABLED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("reason", "hijacking")
				, Set.of("OIDRISC-2.3"));

			case SsfEvents.RISC_ACCOUNT_ENABLED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.4"));

			case SsfEvents.RISC_IDENTIFIER_CHANGED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("new-value", "new-valid")
				, Set.of("OIDRISC-2.5"));

			case SsfEvents.RISC_IDENTIFIER_RECYCLED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.6"));

			case SsfEvents.RISC_CREDENTIAL_COMPROMISE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of("credential_type", "password")
				, Set.of("OIDRISC-2.7"));

			case SsfEvents.RISC_OPT_IN_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.8.1"));

			case SsfEvents.RISC_OPT_OUT_INITIATED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.8.2"));

			case SsfEvents.RISC_OPT_OUT_CANCELLED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.8.3"));

			case SsfEvents.RISC_OPT_OUT_EFFECTIVE_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.8.4"));

			case SsfEvents.RISC_RECOVERY_ACTIVATED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.9"));

			case SsfEvents.RISC_RECOVERY_INFORMATION_CHANGED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.10"));

			case SsfEvents.RISC_SESSIONS_REVOKED_DEPRECATED_EVENT_TYPE -> new SsfEvent(eventType
				, Map.of()
				, Set.of("OIDRISC-2.11", "OIDCAEP-3.1"));

			default -> new SsfEvent(eventType, Map.of(), Set.of());
		};
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null
			&& !eventsEnqueued.isEmpty()
			&& eventsEnqueued.get(createdStreamId) != null
			&& eventsAcked.get(createdStreamId) != null
			&& eventsAcked.get(createdStreamId).containsAll(eventsEnqueued.get(createdStreamId));
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getId(), "Detected acknowledgements for published events.");
		super.fireTestFinished();
	}
}
