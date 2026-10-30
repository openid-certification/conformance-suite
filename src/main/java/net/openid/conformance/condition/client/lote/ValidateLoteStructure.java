package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.X509CertificateUtil;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks the list of trusted entities against the structure ETSI TS 119 602 clauses 6.3 to 6.6
 * and the EU profiles (Annexes D to G) prescribe: the mandatory scheme information fields, the
 * list type, status determination approach, scheme rules URI and territory ("EU") the profile of
 * the slot's list fixes, dates that make sense (issued in the past, a NextUpdate present,
 * in the future and at most six months after issue), the per-entity and per-service shape with a
 * service type identifier the profile allows and at least one parseable X.509 certificate in
 * each service digital identity, and no service status fields (the EU profiles say they shall
 * not be used). Every problem is collected into one finding list. Meant to be called as a
 * WARNING: the scheme operator is a third party, and the anchors are still used.
 *
 * <p>Deviation from clause 6.3.15, which says a list whose NextUpdate has passed shall be
 * discarded: an overdue list is reported here but its anchors are still extracted, the same way
 * a stale VICAL is treated, so one lapsed operator does not fail every participant's test.
 */
public class ValidateLoteStructure extends AbstractLoteCondition {

	/** The ISO 8601 UTC date-time of clause 6.1.3; a small skew for clocks that disagree. */
	private static final Duration CLOCK_SKEW = Duration.ofMinutes(3);

	/** Annex D.2 Table D.1 and its siblings: at most six months between issue and next update. */
	private static final Duration MAX_UPDATE_INTERVAL = Duration.ofDays(184);

	private static final Pattern DATE_TIME = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");

	/** Annex D.2 Table D.1 and its siblings: the territory of every EU list. */
	private static final String SCHEME_TERRITORY = "EU";

	private final Clock clock;

	public ValidateLoteStructure() {
		this(Clock.systemUTC());
	}

	/** For tests that check a fixed list, whose NextUpdate would otherwise pass. */
	ValidateLoteStructure(Clock clock) {
		this.clock = clock;
	}

	@Override
	@PreEnvironment(required = LOTE)
	public Environment evaluate(Environment env) {
		LoteSlot slot = getSlot(env);
		JsonObject payload = getLotePayload(env);
		JsonObject lote = payload.getAsJsonObject("LoTE");
		JsonObject info = getListAndSchemeInformation(env);
		List<String> findings = new ArrayList<>();

		if (info == null) {
			findings.add("ListAndSchemeInformation is missing");
		} else {
			checkListAndSchemeInformation(info, slot, findings);
		}

		JsonElement entities = lote.get("TrustedEntitiesList");
		if (entities != null && entities.isJsonArray()) {
			JsonArray list = entities.getAsJsonArray();
			if (list.isEmpty()) {
				findings.add("TrustedEntitiesList is present but empty; clause 6.4.0 omits it for an empty list");
			}
			for (int i = 0; i < list.size(); i++) {
				checkTrustedEntity(list.get(i), i, slot, findings);
			}
		}

		if (!findings.isEmpty()) {
			JsonArray findingsJson = new JsonArray();
			findings.forEach(findingsJson::add);
			throw error("The list of trusted entities has " + findings.size() + " structural problem(s), listed in the detail below",
				args("findings", findingsJson, "total_findings", findings.size(), "slot", slot.name()));
		}

		logSuccess("The list of trusted entities has the structure ETSI TS 119 602 and its EU profile prescribe",
			args("slot", slot.name(), "type", info.get("LoTEType")));
		return env;
	}

	private void checkListAndSchemeInformation(JsonObject info, LoteSlot slot, List<String> findings) {
		JsonElement version = info.get("LoTEVersionIdentifier");
		if (version == null || !version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()) {
			findings.add("LoTEVersionIdentifier is missing or not a number");
		} else if (OIDFJSON.getInt(version) != 1) {
			findings.add("LoTEVersionIdentifier is " + version + "; the EU profiles require 1");
		}

		JsonElement sequence = info.get("LoTESequenceNumber");
		if (sequence == null || !sequence.isJsonPrimitive() || !sequence.getAsJsonPrimitive().isNumber()) {
			findings.add("LoTESequenceNumber is missing or not a number");
		}

		JsonElement type = info.get("LoTEType");
		if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString()) {
			findings.add("LoTEType is missing or not a string");
		} else if (!slot.expectedLoteType.equals(OIDFJSON.getString(type))) {
			findings.add("LoTEType is '" + OIDFJSON.getString(type) + "' but the " + slot.fieldDescription()
				+ " should point at a list of type '" + slot.expectedLoteType + "'");
		}

		if (firstValue(info.get("SchemeOperatorName")) == null) {
			findings.add("SchemeOperatorName is missing or has no value");
		}
		if (firstValue(info.get("SchemeName")) == null) {
			findings.add("SchemeName is missing or has no value");
		}
		JsonElement territory = info.get("SchemeTerritory");
		if (territory == null || !territory.isJsonPrimitive() || !territory.getAsJsonPrimitive().isString()) {
			findings.add("SchemeTerritory is missing or not a string");
		} else if (!SCHEME_TERRITORY.equals(OIDFJSON.getString(territory))) {
			findings.add("SchemeTerritory is '" + OIDFJSON.getString(territory) + "'; the EU profiles require '" + SCHEME_TERRITORY + "'");
		}
		JsonElement approach = info.get("StatusDeterminationApproach");
		if (approach == null || !approach.isJsonPrimitive() || !approach.getAsJsonPrimitive().isString()) {
			findings.add("StatusDeterminationApproach is missing or not a string");
		} else if (!slot.expectedStatusDeterminationApproach.equals(OIDFJSON.getString(approach))) {
			findings.add("StatusDeterminationApproach is '" + OIDFJSON.getString(approach) + "'; the profile of this list requires '"
				+ slot.expectedStatusDeterminationApproach + "'");
		}
		JsonElement rules = info.get("SchemeTypeCommunityRules");
		if (rules == null) {
			findings.add("SchemeTypeCommunityRules is missing");
		} else if (!uriValues(rules).contains(slot.expectedSchemeTypeCommunityRules)) {
			findings.add("SchemeTypeCommunityRules " + uriValues(rules) + " does not include '" + slot.expectedSchemeTypeCommunityRules
				+ "', which the profile of this list requires");
		}
		if (info.get("SchemeInformationURI") == null) {
			findings.add("SchemeInformationURI is missing");
		}

		Instant now = clock.instant();
		Instant issued = dateTime(info.get("ListIssueDateTime"), "ListIssueDateTime", findings);
		// clause 6.3.15: null would mean a closed list, which is no source of trust either
		Instant nextUpdate = dateTime(info.get("NextUpdate"), "NextUpdate", findings);
		if (issued != null && issued.isAfter(now.plus(CLOCK_SKEW))) {
			findings.add("ListIssueDateTime " + issued + " is in the future");
		}
		if (nextUpdate != null && nextUpdate.isBefore(now.minus(CLOCK_SKEW))) {
			findings.add("NextUpdate " + nextUpdate + " has passed; clause 6.3.15 says such a list shall be discarded as expired");
		}
		if (issued != null && nextUpdate != null) {
			if (!nextUpdate.isAfter(issued)) {
				findings.add("NextUpdate " + nextUpdate + " is not after ListIssueDateTime " + issued);
			} else if (Duration.between(issued, nextUpdate).compareTo(MAX_UPDATE_INTERVAL) > 0) {
				findings.add("NextUpdate " + nextUpdate + " is more than six months after ListIssueDateTime " + issued
					+ "; the EU profiles cap the interval at six months");
			}
		}
	}

	private void checkTrustedEntity(JsonElement entityEl, int index, LoteSlot slot, List<String> findings) {
		String prefix = "TrustedEntitiesList[" + index + "]: ";
		if (!entityEl.isJsonObject()) {
			findings.add(prefix + "entry is not an object");
			return;
		}
		JsonObject entity = entityEl.getAsJsonObject();
		JsonElement entityInfo = entity.get("TrustedEntityInformation");
		String name = null;
		if (entityInfo == null || !entityInfo.isJsonObject()) {
			findings.add(prefix + "TrustedEntityInformation is missing");
		} else {
			name = firstValue(entityInfo.getAsJsonObject().get("TEName"));
			if (name == null) {
				findings.add(prefix + "TEName is missing or has no value");
			}
		}
		if (name != null) {
			prefix = "trusted entity '" + name + "': ";
		}

		JsonElement services = entity.get("TrustedEntityServices");
		if (services == null || !services.isJsonArray() || services.getAsJsonArray().isEmpty()) {
			findings.add(prefix + "TrustedEntityServices is missing or empty");
			return;
		}
		int i = 0;
		for (JsonElement serviceEl : services.getAsJsonArray()) {
			checkService(serviceEl, prefix + "service " + i + ": ", slot, findings);
			i++;
		}
	}

	private void checkService(JsonElement serviceEl, String prefix, LoteSlot slot, List<String> findings) {
		if (!serviceEl.isJsonObject()) {
			findings.add(prefix + "entry is not an object");
			return;
		}
		// clause 6.4.3: each service is a ServiceInformation (plus optional ServiceHistory)
		JsonElement infoEl = serviceEl.getAsJsonObject().get("ServiceInformation");
		if (infoEl == null || !infoEl.isJsonObject()) {
			findings.add(prefix + "ServiceInformation is missing");
			return;
		}
		JsonObject service = infoEl.getAsJsonObject();

		JsonElement typeEl = service.get("ServiceTypeIdentifier");
		if (typeEl == null || !typeEl.isJsonPrimitive() || !typeEl.getAsJsonPrimitive().isString()) {
			findings.add(prefix + "ServiceTypeIdentifier is missing or not a string");
		} else if (!slot.acceptsServiceType(OIDFJSON.getString(typeEl))) {
			findings.add(prefix + "ServiceTypeIdentifier '" + OIDFJSON.getString(typeEl)
				+ "' is not one the profile of this list allows " + slot.allowedServiceTypes);
		}
		if (firstValue(service.get("ServiceName")) == null) {
			findings.add(prefix + "ServiceName is missing or has no value");
		}

		JsonElement identity = service.get("ServiceDigitalIdentity");
		if (identity == null || !identity.isJsonObject()) {
			findings.add(prefix + "ServiceDigitalIdentity is missing");
		} else {
			JsonElement certs = identity.getAsJsonObject().get("X509Certificates");
			if (certs == null || !certs.isJsonArray() || certs.getAsJsonArray().isEmpty()) {
				findings.add(prefix + "ServiceDigitalIdentity has no X509Certificates; clause 6.6.0 requires at least one for a PKI based service");
			} else {
				int c = 0;
				for (JsonElement certEl : certs.getAsJsonArray()) {
					JsonElement val = certEl.isJsonObject() ? certEl.getAsJsonObject().get("val") : null;
					if (val == null || !val.isJsonPrimitive() || !val.getAsJsonPrimitive().isString()) {
						findings.add(prefix + "X509Certificates[" + c + "] has no 'val' string");
					} else {
						if (X509CertificateUtil.parseBase64Der(OIDFJSON.getString(val)) == null) {
							findings.add(prefix + "X509Certificates[" + c + "] is not a base64 DER X.509 certificate");
						}
					}
					c++;
				}
			}
		}

		// Annex D.3 Table D.3 and its siblings: the EU profiles do not use the status fields
		for (String forbidden : List.of("ServiceStatus", "StatusStartingTime")) {
			if (service.has(forbidden)) {
				findings.add(prefix + forbidden + " is present; the EU profiles say it shall not be used");
			}
		}
	}

	/** The uriValue members of a multilingual pointer array (Annex B.3); empty when the element has another shape. */
	private static List<String> uriValues(JsonElement multilingual) {
		List<String> values = new ArrayList<>();
		if (multilingual.isJsonArray()) {
			for (JsonElement entry : multilingual.getAsJsonArray()) {
				JsonElement uri = entry.isJsonObject() ? entry.getAsJsonObject().get("uriValue") : null;
				if (uri != null && uri.isJsonPrimitive() && uri.getAsJsonPrimitive().isString()) {
					values.add(OIDFJSON.getString(uri));
				}
			}
		}
		return values;
	}

	private static Instant dateTime(JsonElement el, String field, List<String> findings) {
		if (el == null || el.isJsonNull()) {
			findings.add(field + " is missing or null");
			return null;
		}
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
			findings.add(field + " is not a string");
			return null;
		}
		String value = OIDFJSON.getString(el);
		// clause 6.1.3: second precision, no fractional part, UTC marked with 'Z'
		if (!DATE_TIME.matcher(value).matches()) {
			findings.add(field + " '" + value + "' is not a UTC date-time of the form YYYY-MM-DDThh:mm:ssZ (clause 6.1.3)");
			return null;
		}
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException e) {
			findings.add(field + " '" + value + "' is not a valid date-time (clause 6.1.3)");
			return null;
		}
	}
}
