package net.openid.conformance.openid.ssf;

import java.util.LinkedHashSet;
import java.util.Set;

public class SsfEvents {

	public static final String SSF_STREAM_VERIFICATION_EVENT_TYPE = "https://schemas.openid.net/secevent/ssf/event-type/verification";

	public static final String SSF_STREAM_UPDATED_EVENT_TYPE = "https://schemas.openid.net/secevent/ssf/event-type/stream-updated";

	public static final Set<String> SSF_EVENT_TYPES = Set.of(
		// see: https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#section-8.1.4.2
		SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE,
		// see: https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#name-stream-updated-event
		SsfEvents.SSF_STREAM_UPDATED_EVENT_TYPE
	);

	public static final String CAEP_SESSION_REVOKED_EVENT_TYPE = "https://schemas.openid.net/secevent/caep/event-type/session-revoked";

	public static final String CAEP_TOKEN_CLAIMS_CHANGE_EVENT_TYPE = "https://schemas.openid.net/secevent/caep/event-type/token-claims-change";

	public static final String CAEP_CREDENTIAL_CHANGE_EVENT_TYPE = "https://schemas.openid.net/secevent/caep/event-type/credential-change";

	public static final String CAEP_ASSURANCE_LEVEL_CHANGE_EVENT_TYPE = "https://schemas.openid.net/secevent/caep/event-type/assurance-level-change";

	public static final String CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE = "https://schemas.openid.net/secevent/caep/event-type/device-compliance-change";

	public static final String CAEP_SESSION_ESTABLISHED_EVENT_TYPE = "https://schemas.openid.net/secevent/caep/event-type/session-established";

	public static final String CAEP_SESSION_PRESENTED_EVENT_TYPE = "https://schemas.openid.net/secevent/caep/event-type/session-presented";

	public static final String CAEP_RISK_LEVEL_CHANGE_EVENT_TYPE = "https://schemas.openid.net/secevent/caep/event-type/risk-level-change";

	public static final Set<String> CAEP_EVENT_TYPES = Set.of( //
		// see: https://openid.net/specs/openid-caep-1_0-final.html
		CAEP_SESSION_REVOKED_EVENT_TYPE, //
		CAEP_TOKEN_CLAIMS_CHANGE_EVENT_TYPE, //
		CAEP_CREDENTIAL_CHANGE_EVENT_TYPE, //
		CAEP_ASSURANCE_LEVEL_CHANGE_EVENT_TYPE, //
		CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE, //
		CAEP_SESSION_ESTABLISHED_EVENT_TYPE, //
		CAEP_SESSION_PRESENTED_EVENT_TYPE, //
		CAEP_RISK_LEVEL_CHANGE_EVENT_TYPE //
	);

	public static final String RISC_ACCOUNT_CREDENTIAL_CHANGE_REQUIRED_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/account-credential-change-required";

	public static final String RISC_ACCOUNT_PURGED_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/account-purged";

	public static final String RISC_ACCOUNT_DISABLED_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/account-disabled";

	public static final String RISC_ACCOUNT_ENABLED_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/account-enabled";

	public static final String RISC_IDENTIFIER_CHANGED_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/identifier-changed";

	public static final String RISC_IDENTIFIER_RECYCLED_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/identifier-recycled";

	public static final String RISC_CREDENTIAL_COMPROMISE_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/credential-compromise";

	public static final String RISC_OPT_IN_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/opt-in";

	public static final String RISC_OPT_OUT_INITIATED_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/opt-out-initiated";

	public static final String RISC_OPT_OUT_CANCELLED_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/opt-out-cancelled";

	public static final String RISC_OPT_OUT_EFFECTIVE_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/opt-out-effective";

	public static final String RISC_RECOVERY_ACTIVATED_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/recovery-activated";

	public static final String RISC_RECOVERY_INFORMATION_CHANGED_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/recovery-information-changed";

	public static final String RISC_SESSIONS_REVOKED_DEPRECATED_EVENT_TYPE = "https://schemas.openid.net/secevent/risc/event-type/sessions-revoked";

	public static final Set<String> RISC_EVENT_TYPES = Set.of( //
		// RISC events
		RISC_ACCOUNT_CREDENTIAL_CHANGE_REQUIRED_EVENT_TYPE, //
		RISC_ACCOUNT_DISABLED_EVENT_TYPE, //
		RISC_ACCOUNT_ENABLED_EVENT_TYPE, //
		RISC_ACCOUNT_PURGED_EVENT_TYPE, //
		RISC_CREDENTIAL_COMPROMISE_EVENT_TYPE, //
		RISC_IDENTIFIER_CHANGED_EVENT_TYPE, //
		RISC_IDENTIFIER_RECYCLED_EVENT_TYPE, //
		RISC_OPT_IN_EVENT_TYPE, //
		RISC_OPT_OUT_CANCELLED_EVENT_TYPE, //
		RISC_OPT_OUT_EFFECTIVE_EVENT_TYPE, //
		RISC_OPT_OUT_INITIATED_EVENT_TYPE, //
		RISC_RECOVERY_ACTIVATED_EVENT_TYPE, //
		RISC_RECOVERY_INFORMATION_CHANGED_EVENT_TYPE, //

		// deprecated new impls should use the CAEP SESSION_REVOKED event
		RISC_SESSIONS_REVOKED_DEPRECATED_EVENT_TYPE //
		);

	public static final String SCIM_FEED_ADD_EVENT_TYPE = "urn:ietf:params:scim:event:feed:add";

	public static final String SCIM_FEED_REMOVE_EVENT_TYPE = "urn:ietf:params:scim:event:feed:remove";

	public static final String SCIM_PROV_CREATE_NOTICE_EVENT_TYPE = "urn:ietf:params:scim:event:prov:create:notice";

	public static final String SCIM_PROV_CREATE_FULL_EVENT_TYPE = "urn:ietf:params:scim:event:prov:create:full";

	public static final String SCIM_PROV_PATCH_NOTICE_EVENT_TYPE = "urn:ietf:params:scim:event:prov:patch:notice";

	public static final String SCIM_PROV_PATCH_FULL_EVENT_TYPE = "urn:ietf:params:scim:event:prov:patch:full";

	public static final String SCIM_PROV_PUT_NOTICE_EVENT_TYPE = "urn:ietf:params:scim:event:prov:put:notice";

	public static final String SCIM_PROV_PUT_FULL_EVENT_TYPE = "urn:ietf:params:scim:event:prov:put:full";

	public static final String SCIM_PROV_DELETE_EVENT_TYPE = "urn:ietf:params:scim:event:prov:delete";

	public static final String SCIM_PROV_ACTIVATE_EVENT_TYPE = "urn:ietf:params:scim:event:prov:activate";

	public static final String SCIM_PROV_DEACTIVATE_EVENT_TYPE = "urn:ietf:params:scim:event:prov:deactivate";

	public static final String SCIM_MISC_ASYNCRESP_EVENT_TYPE = "urn:ietf:params:scim:event:misc:asyncresp";

	public static final Set<String> SCIM_EVENT_TYPES = Set.of( //
		// see: https://www.rfc-editor.org/rfc/rfc9967.html#name-initial-contents-of-the-sci
		SCIM_FEED_ADD_EVENT_TYPE, //
		SCIM_FEED_REMOVE_EVENT_TYPE, //
		SCIM_PROV_CREATE_NOTICE_EVENT_TYPE, //
		SCIM_PROV_CREATE_FULL_EVENT_TYPE, //
		SCIM_PROV_PATCH_NOTICE_EVENT_TYPE, //
		SCIM_PROV_PATCH_FULL_EVENT_TYPE, //
		SCIM_PROV_PUT_NOTICE_EVENT_TYPE, //
		SCIM_PROV_PUT_FULL_EVENT_TYPE, //
		SCIM_PROV_DELETE_EVENT_TYPE, //
		SCIM_PROV_ACTIVATE_EVENT_TYPE, //
		SCIM_PROV_DEACTIVATE_EVENT_TYPE, //
		SCIM_MISC_ASYNCRESP_EVENT_TYPE //
	);

	/**
	 * Combined SSF, CAEP, RISC and SCIM events.
	 */
	public static final Set<String> STANDARD_EVENT_TYPES;
	static {
		Set<String> events = new LinkedHashSet<>();
		events.addAll(SsfEvents.SSF_EVENT_TYPES);
		events.addAll(SsfEvents.CAEP_EVENT_TYPES);
		events.addAll(SsfEvents.RISC_EVENT_TYPES);
		events.addAll(SsfEvents.SCIM_EVENT_TYPES);
		STANDARD_EVENT_TYPES = Set.copyOf(events);
	}

	public static final Set<String> CAEP_INTEROP_EVENT_TYPES;
	static {
		Set<String> events = new LinkedHashSet<>();
		events.add(CAEP_SESSION_REVOKED_EVENT_TYPE);
		events.add(CAEP_CREDENTIAL_CHANGE_EVENT_TYPE);
		events.add(CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE); // see: https://github.com/openid/sharedsignals/issues/311
		CAEP_INTEROP_EVENT_TYPES = events;
	}

	public static boolean isVerificationEvent(String type) {
		return SSF_STREAM_VERIFICATION_EVENT_TYPE.equals(type);
	}
}
