package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import kotlinx.io.bytestring.ByteString;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.Crypto;
import org.multipaz.mdoc.mso.MobileSecurityObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Checks that every IssuerSignedItem in an issued mdoc credential's nameSpaces is covered by a
 * matching digest in the signed Mobile Security Object, per ISO/IEC 18013-5:2021 §9.1.2.4 (the
 * digest is calculated over the IssuerSignedItemBytes using the MSO's digestAlgorithm). Without
 * this check an element could be present in nameSpaces without being protected by the issuer's
 * signature, and a verifier would reject it at presentation time.
 *
 * The COSE_Sign1 signature over the MSO itself is checked separately by
 * {@link ValidateMdocIssuerSignedSignature}.
 *
 * Reads the raw CBOR stored as standard base64 in 'mdoc_credential_cbor' by
 * ParseMdocCredentialFromVCIIssuance.
 */
public class ValidateMdocIssuerSignedItemDigests extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "mdoc_credential_cbor")
	public Environment evaluate(Environment env) {

		String digestAlgorithm;
		List<String> problems = new ArrayList<>();
		Map<String, List<String>> verifiedElements = new TreeMap<>();
		try {
			byte[] bytes = Base64.getDecoder().decode(env.getString("mdoc_credential_cbor"));
			DataItem issuerSigned = Cbor.INSTANCE.decode(bytes);
			MobileSecurityObject mso = MdocUtil.parseMso(issuerSigned);
			Algorithm algorithm = mso.getDigestAlgorithm();
			digestAlgorithm = algorithm.name();
			Map<String, Map<Long, ByteString>> valueDigests = mso.getValueDigests();

			Map<String, List<DataItem>> itemsByNamespace = MdocUtil.getIssuerSignedItems(issuerSigned);
			if (itemsByNamespace.isEmpty()) {
				log("The mdoc credential contains no nameSpaces, so there are no IssuerSignedItem digests to check");
				return env;
			}

			for (Map.Entry<String, List<DataItem>> namespaceEntry : itemsByNamespace.entrySet()) {
				String namespace = namespaceEntry.getKey();
				Map<Long, ByteString> namespaceDigests = valueDigests.get(namespace);
				for (DataItem issuerSignedItemBytes : namespaceEntry.getValue()) {
					DataItem issuerSignedItem = issuerSignedItemBytes.getAsTaggedEncodedCbor();
					String elementIdentifier = issuerSignedItem.get("elementIdentifier").getAsTstr();
					long digestId = issuerSignedItem.get("digestID").getAsNumber();
					byte[] computedDigest = digest(algorithm, Cbor.INSTANCE.encode(issuerSignedItemBytes));
					ByteString msoDigest = namespaceDigests == null ? null : namespaceDigests.get(digestId);
					if (msoDigest == null) {
						problems.add("'" + namespace + "' / '" + elementIdentifier
							+ "': the signed MSO contains no digest for digestID " + digestId);
					} else if (!msoDigest.equals(new ByteString(computedDigest, 0, computedDigest.length))) {
						problems.add("'" + namespace + "' / '" + elementIdentifier
							+ "': the digest of the IssuerSignedItemBytes does not match the MSO digest for digestID " + digestId);
					} else {
						verifiedElements.computeIfAbsent(namespace, k -> new ArrayList<>()).add(elementIdentifier);
					}
				}
			}
		} catch (ConditionError e) {
			throw e;
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		} catch (Exception e) {
			throw error("Failed to parse the mdoc credential", e);
		}

		if (!problems.isEmpty()) {
			throw error("One or more IssuerSignedItems in the mdoc credential are not covered by a matching digest "
					+ "in the signed Mobile Security Object",
				args("problems", problems, "digest_algorithm", digestAlgorithm));
		}

		logSuccess("Every IssuerSignedItem in the mdoc credential matches its digest in the signed Mobile Security Object",
			args("verified_elements", verifiedElements, "digest_algorithm", digestAlgorithm));

		return env;
	}

	private byte[] digest(Algorithm algorithm, byte[] data) {
		try {
			return kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				(scope, continuation) -> Crypto.INSTANCE.digest(algorithm, data, continuation));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw error("Interrupted while computing IssuerSignedItem digest", e);
		}
	}
}
