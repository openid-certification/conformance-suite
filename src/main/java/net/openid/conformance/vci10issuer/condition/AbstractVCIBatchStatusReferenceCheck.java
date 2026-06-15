package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonObjectUtils;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.mdoc.mso.MobileSecurityObject;
import org.multipaz.revocation.RevocationStatus;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Base for the batch status-list unlinkability checks. Reads the credentials captured for the batch
 * (the whole-credential {@code linkability_captures} list populated by
 * {@link VCICaptureCredentialForLinkability} during per-credential verification) and extracts each
 * credential's Token Status List reference (uri + index):
 *
 * <ul>
 *   <li>SD-JWT VC: the {@code status.status_list} claim.</li>
 *   <li>mdoc: the MSO {@code status} (a {@code RevocationStatus.StatusList}).</li>
 * </ul>
 *
 * Credentials with no status reference are omitted. Subclasses assert the unlinkability properties
 * required by RFC 9901 §10.1 / HAIP §6.1 / Token Status List §12.5 over the collected references.
 */
public abstract class AbstractVCIBatchStatusReferenceCheck extends AbstractCondition {

	/** A credential's status list reference, with the index of the credential in the batch. */
	protected record StatusRef(String uri, long idx, int credentialIndex) {
	}

	protected List<StatusRef> extractStatusReferences(Environment env) {
		List<StatusRef> refs = new ArrayList<>();
		JsonElement listEl = env.getElementFromObject("linkability_captures", "list");
		if (listEl == null || !listEl.isJsonArray()) {
			return refs;
		}
		JsonArray list = listEl.getAsJsonArray();
		for (int i = 0; i < list.size(); i++) {
			JsonObject capture = list.get(i).getAsJsonObject();
			StatusRef ref = "mso_mdoc".equals(OIDFJSON.getStringOrNull(capture.get("format")))
				? extractMdocStatus(capture, i)
				: extractSdJwtStatus(capture, i);
			if (ref != null) {
				refs.add(ref);
			}
		}
		return refs;
	}

	private StatusRef extractSdJwtStatus(JsonObject capture, int index) {
		JsonElement statusList = JsonObjectUtils.path(capture, "sdjwt", "credential", "claims", "status", "status_list");
		if (statusList == null || !statusList.isJsonObject()) {
			return null;
		}
		JsonObject sl = statusList.getAsJsonObject();
		if (!sl.has("uri") || !sl.has("idx") || sl.get("uri").isJsonNull() || sl.get("idx").isJsonNull()) {
			return null;
		}
		try {
			return new StatusRef(OIDFJSON.getString(sl.get("uri")), OIDFJSON.getLong(sl.get("idx")), index);
		} catch (OIDFJSON.UnexpectedJsonTypeException e) {
			throw error("The credential's status_list 'uri' or 'idx' is not of the expected type "
					+ "('uri' must be a string, 'idx' a number)", e,
				args("status_list", sl, "credential_index", index));
		}
	}

	private StatusRef extractMdocStatus(JsonObject capture, int index) {
		String cbor = OIDFJSON.getStringOrNull(capture.get("mdoc_credential_cbor"));
		if (cbor == null) {
			return null;
		}
		// a capture whose mdoc can't be parsed is omitted, like one with no status reference
		MobileSecurityObject mso;
		try {
			mso = MdocUtil.parseMso(Base64.getDecoder().decode(cbor));
		} catch (MdocUtil.MdocParseException | IllegalArgumentException e) {
			return null;
		}
		if (mso.getRevocationStatus() instanceof RevocationStatus.StatusList statusList) {
			return new StatusRef(statusList.getUri(), statusList.getIdx(), index);
		}
		return null;
	}

}
