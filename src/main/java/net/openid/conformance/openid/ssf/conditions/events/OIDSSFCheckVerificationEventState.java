package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class OIDSSFCheckVerificationEventState extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonObject claimsJsonObject = env.getElementFromObject("ssf", "verification.token.claims").getAsJsonObject();

		JsonObject eventsObject = claimsJsonObject.getAsJsonObject("events");
		if (eventsObject == null) {
			throw error("Missing events claim", args("token_claims", claimsJsonObject));
		}

		JsonElement verificationEventEL = eventsObject.get(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE);
		if (verificationEventEL == null) {
			throw error("Missing '" + SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE + "' in events claim", args("token_claims", claimsJsonObject));
		}

		JsonObject verificationEventObject = verificationEventEL.getAsJsonObject();

		// SSF 1.0 §8.1.4 and §8.1.4.2 together imply:
		//   - §8.1.4: "A Transmitter MAY send a Verification Event at any time,
		//     even if one was not requested by the Event Receiver." (i.e. transmitter-
		//     initiated / unsolicited events are always legitimate.)
		//   - §8.1.4.2: "If the Verification Event is initiated by the Transmitter
		//     then this parameter [state] MUST not be set." (i.e. unsolicited events
		//     MUST NOT carry state.)
		//   - §8.1.4.2: "[state is] an arbitrary string that the Event Transmitter
		//     MUST echo back to the Event Receiver in the Verification Event's
		//     payload." (i.e. when the receiver sent a state in its verification
		//     request, the transmitter MUST include that same state in the response.)
		//
		// So the event's own "state" claim is the authoritative indicator of whether
		// the event is a solicited response or a transmitter-initiated delivery:
		//   - absent  -> transmitter-initiated (unsolicited). Accept it — it is NOT
		//                the response to any verification request we may have issued,
		//                so ssf.verification.state is not relevant here.
		//   - present -> solicited. It MUST match a state we sent: normally
		//                ssf.verification.state (the latest verification request), but a
		//                late echo of an earlier request (ssf.verification.issued_states)
		//                is equally legitimate, since §8.1.4.2 also says receivers "MUST
		//                NOT depend on the Verification Event being transmitted
		//                synchronously or in any particular order". If no verification
		//                request has been issued, a transmitter echoing a state is
		//                an error (the transmitter would have violated §8.1.4.2's
		//                "MUST not be set" clause for its own unsolicited events).
		//
		// Note: this condition does not enforce §8.1.4.2's "MUST echo back" clause on
		// its own — that requires the caller to loop until a stated event arrives
		// (see AbstractOIDSSFTransmitterStreamVerificationTest and the push/poll
		// test loops). A single call here cannot distinguish "legitimate unsolicited
		// event" from "transmitter failed to echo state".
		JsonElement stateEl = verificationEventObject.get("state");
		if (stateEl == null) {
			logSuccess("Verification event carries no state; treated as transmitter-initiated (unsolicited)",
				args("token_claims", claimsJsonObject));
			return env;
		}

		String actualVerificationState = OIDFJSON.getString(stateEl);
		String expectedVerificationState = env.getString("ssf", "verification.state");
		List<String> issuedStates = issuedVerificationStates(env, expectedVerificationState);
		if (issuedStates.isEmpty()) {
			throw error("Verification event carries a state but the receiver has not issued a verification request",
				args("token_claims", claimsJsonObject, "actual_state", actualVerificationState));
		}

		if (actualVerificationState.equals(expectedVerificationState)) {
			logSuccess("Retrieved verification state matches expected verification state",
				args("expected_state", expectedVerificationState, "actual_state", actualVerificationState));
			return env;
		}

		if (issuedStates.contains(actualVerificationState)) {
			logSuccess("Retrieved verification state matches an earlier verification request of this test; "
					+ "verification events may arrive out of order",
				args("expected_state", expectedVerificationState, "actual_state", actualVerificationState, "issued_states", issuedStates));
			return env;
		}

		throw error("Retrieved verification state does not match any verification state issued by this test",
			args("expected_state", expectedVerificationState, "actual_state", actualVerificationState,
				"issued_states", issuedStates, "token_claims", claimsJsonObject));
	}

	/**
	 * Every state this test has sent in a verification request, latest last. Falls back to
	 * the single latest state when no list was recorded.
	 */
	private static List<String> issuedVerificationStates(Environment env, String latestState) {
		JsonElement issuedEl = env.getElementFromObject("ssf", "verification.issued_states");
		if (issuedEl != null && issuedEl.isJsonArray()) {
			return OIDFJSON.convertJsonArrayToList(issuedEl.getAsJsonArray());
		}
		return latestState == null ? List.of() : List.of(latestState);
	}

}
