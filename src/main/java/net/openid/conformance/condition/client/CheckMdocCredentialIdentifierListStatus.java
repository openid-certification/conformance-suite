package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.DataItem;

import java.util.Base64;
import java.util.Map;

/**
 * Reads the mdoc's status from the fetched identifier list and fails when the MSO has been
 * revoked. ISO/IEC 18013-5 12.3.6.4: "Presence of the Identifier in the IdentifierList indicates
 * that the MSO that contains that identifier in the status element is revoked." 12.3.6.1 adds
 * that for an mdoc "it implies that only revoked MSOs ... are put on the identifier list", so
 * being listed is unambiguously a revocation.
 */
public class CheckMdocCredentialIdentifierListStatus extends AbstractIdentifierListCwtCondition {

	@Override
	@PreEnvironment(strings = { ENV_IDENTIFIER_LIST_TOKEN, ENV_IDENTIFIER_LIST_ID })
	public Environment evaluate(Environment env) {
		ParsedStatusListCwt parsed = parseStatusListCwt(env);

		byte[] identifier = getMsoIdentifier(env);

		Map<DataItem, DataItem> identifiers = getIdentifiers(parsed.claims());
		if (identifiers == null) {
			throw error("The MSO revocation list does not contain an IdentifierList claim (key 65530)"
				+ " with an 'identifiers' map, so the mdoc's revocation status cannot be determined",
				args("uri", env.getString(ENV_IDENTIFIER_LIST_URI)));
		}

		String id = Base64.getUrlEncoder().withoutPadding().encodeToString(identifier);

		if (containsIdentifier(identifiers, identifier)) {
			throw error("The mdoc's MSO has been revoked: its identifier is present in the MSO"
				+ " revocation list referenced by the MSO's identifier_list element",
				args("id", id, "uri", env.getString(ENV_IDENTIFIER_LIST_URI)));
		}

		logSuccess("The mdoc's MSO is not revoked: its identifier is not present in the MSO"
			+ " revocation list", args("id", id,
				"uri", env.getString(ENV_IDENTIFIER_LIST_URI),
				"identifiers", identifiers.size()));
		return env;
	}
}
