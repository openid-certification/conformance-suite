package net.openid.conformance.vci10issuer.condition;

import com.authlete.sd.SDJWT;
import com.authlete.sd.SDObjectDecoder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.JWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks that all SD-JWT credentials issued in a batch contain the same Credential Dataset.
 *
 * OID4VCI 1.0 Final §3.3.2: "In the context of a single request, the batch of issued
 * Credentials sent in response MUST share the same Credential Format and Credential
 * Dataset, but SHOULD contain different Cryptographic Data."
 *
 * The comparison is done on the claims with all disclosures applied, ignoring claims that
 * legitimately differ between the credentials of a batch: the key binding (cnf), times
 * (iat/nbf/exp), token id (jti) and per-credential status information (status), plus any
 * leftover selective disclosure artifacts.
 */
public class VCIEnsureBatchSdJwtCredentialDatasetsMatch extends AbstractCondition {

	private static final Set<String> CLAIMS_THAT_MAY_DIFFER =
		Set.of("cnf", "iat", "nbf", "exp", "jti", "status", "_sd", "_sd_alg", "sd_hash");

	private static final Gson GSON = new Gson();

	@Override
	@PreEnvironment(required = "extracted_credentials")
	public Environment evaluate(Environment env) {

		JsonArray list = env.getObject("extracted_credentials").getAsJsonArray("list");
		if (list.isEmpty()) {
			throw error("No credentials found in 'extracted_credentials' - there is nothing to compare");
		}

		List<JsonObject> datasets = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			datasets.add(extractDataset(OIDFJSON.getString(list.get(i)), i));
		}

		JsonObject firstDataset = datasets.get(0);
		for (int i = 1; i < datasets.size(); i++) {
			if (!firstDataset.equals(datasets.get(i))) {
				throw error("Credentials issued in the same batch must contain the same Credential Dataset, "
						+ "but the dataset of one credential differs from the first credential. "
						+ "(The comparison ignores cnf, iat, nbf, exp, jti and status, which may legitimately differ.)",
					args("credential_index", i,
						"first_credential_dataset", firstDataset,
						"differing_credential_dataset", datasets.get(i)));
			}
		}

		logSuccess("All " + datasets.size() + " credentials in the batch contain the same Credential Dataset",
			args("credential_dataset", firstDataset));

		return env;
	}

	private JsonObject extractDataset(String sdJwtString, int index) {
		SDJWT sdJwt;
		try {
			sdJwt = SDJWT.parse(sdJwtString);
		} catch (IllegalArgumentException e) {
			throw error("Parsing SD-JWT failed", e, args("credential_index", index, "credential", sdJwtString));
		}

		Map<String, Object> decoded;
		try {
			JWT credentialJwt = JWTUtil.parseJWT(sdJwt.getCredentialJwt());
			decoded = new SDObjectDecoder().decode(
				credentialJwt.getJWTClaimsSet().toJSONObject(true),
				sdJwt.getDisclosures());
		} catch (ParseException e) {
			throw error("Applying SD-JWT disclosures failed", e,
				args("credential_index", index, "credential", sdJwtString));
		}

		JsonObject dataset = JsonParser.parseString(GSON.toJson(decoded)).getAsJsonObject();
		for (String claim : CLAIMS_THAT_MAY_DIFFER) {
			dataset.remove(claim);
		}
		return dataset;
	}
}
