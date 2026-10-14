package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jose.JWSObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Parses the fetched list of trusted entities as a compact JWS whose payload is the JSON binding
 * of ETSI TS 119 602 (Annex A.1): an object with a {@code LoTE} member holding
 * {@code ListAndSchemeInformation} and, unless the list is empty (clause 6.4.0),
 * {@code TrustedEntitiesList}. Stores the header and payload for the later checks and logs a
 * summary. Signature verification is done separately by {@link ValidateLoteSignature} so a
 * broken signature surfaces as its own finding.
 */
public class ParseLote extends AbstractLoteCondition {

	@Override
	@PreEnvironment(required = LOTE)
	@PostEnvironment(required = LOTE)
	public Environment evaluate(Environment env) {
		JWSObject jws = parseJws(env);

		JsonObject header;
		JsonObject payload;
		try {
			header = JsonParser.parseString(jws.getHeader().toString()).getAsJsonObject();
			JsonElement payloadEl = JsonParser.parseString(jws.getPayload().toString());
			if (!payloadEl.isJsonObject()) {
				throw error("The JWS payload of the list of trusted entities is not a JSON object",
					args("payload", jws.getPayload().toString()));
			}
			payload = payloadEl.getAsJsonObject();
		} catch (JsonSyntaxException | IllegalStateException e) {
			throw error("The JWS payload of the list of trusted entities is not JSON", e,
				args("payload", jws.getPayload().toString()));
		}

		JsonElement lote = payload.get("LoTE");
		if (lote == null || !lote.isJsonObject()) {
			throw error("The JWS payload of the list of trusted entities has no 'LoTE' object (ETSI TS 119 602 Annex A.1 JSON binding)",
				args("payload", payload));
		}
		JsonElement info = lote.getAsJsonObject().get("ListAndSchemeInformation");
		if (info == null || !info.isJsonObject()) {
			throw error("The list of trusted entities has no 'ListAndSchemeInformation' object", args("lote", lote));
		}
		JsonElement entities = lote.getAsJsonObject().get("TrustedEntitiesList");
		if (entities != null && !entities.isJsonArray()) {
			throw error("The list of trusted entities' 'TrustedEntitiesList' is not an array", args("lote", lote));
		}

		env.putObject(LOTE, "header", header);
		env.putObject(LOTE, "payload", payload);

		JsonObject infoObj = info.getAsJsonObject();
		int entityCount = entities == null ? 0 : entities.getAsJsonArray().size();
		int serviceCount = 0;
		JsonArray entityNames = new JsonArray();
		if (entities != null) {
			for (JsonElement entity : entities.getAsJsonArray()) {
				if (!entity.isJsonObject()) {
					continue;
				}
				JsonElement entityInfo = entity.getAsJsonObject().get("TrustedEntityInformation");
				if (entityInfo != null && entityInfo.isJsonObject()) {
					String name = firstValue(entityInfo.getAsJsonObject().get("TEName"));
					entityNames.add(name == null ? "(unnamed)" : name);
				}
				JsonElement services = entity.getAsJsonObject().get("TrustedEntityServices");
				if (services != null && services.isJsonArray()) {
					serviceCount += services.getAsJsonArray().size();
				}
			}
		}

		logSuccess("Parsed the list of trusted entities",
			args("type", infoObj.get("LoTEType"),
				"scheme_operator", firstValue(infoObj.get("SchemeOperatorName")),
				"scheme_name", firstValue(infoObj.get("SchemeName")),
				"sequence_number", infoObj.get("LoTESequenceNumber"),
				"issued", infoObj.get("ListIssueDateTime"),
				"next_update", infoObj.get("NextUpdate"),
				"trusted_entities", entityCount,
				"trusted_entity_names", entityNames,
				"services", serviceCount,
				"header", header));
		return env;
	}
}
