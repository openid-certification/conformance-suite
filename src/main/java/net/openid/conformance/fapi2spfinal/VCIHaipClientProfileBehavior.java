package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * Profile behavior for VCI HAIP (High Assurance Interoperability Profile) client tests.
 *
 * <p>HAIP wallet constraints (DPoP-only sender constraining, client_attestation auth,
 * unsigned auth requests, plain OAuth without id_token, no JARM) are enforced via the
 * variant configuration in {@link net.openid.conformance.vci10wallet.VCIWalletTestPlanHaip}.
 * The issuer-side server-emulator differences today are:
 * <ul>
 *   <li>Inject the HAIP {@code status_list} claim into issued SD-JWT credentials —
 *       see {@link #additionalSdJwtClaims()}.</li>
 *   <li>Initialise the placeholder status list aggregation under {@code env.vci.status_lists}
 *       and advertise {@code server.status_list_aggregation_endpoint} —
 *       see {@link #initializeStatusListState(Environment)}.</li>
 * </ul>
 */
public class VCIHaipClientProfileBehavior extends VCIClientProfileBehavior {

	/**
	 * Inject the HAIP-required SD-JWT status list reference so paired wallet tests
	 * receive the same credential shape regardless of which side serves the issuer.
	 * Delegates to {@link #buildSdJwtStatusListClaims(Environment)} so the wallet's
	 * imperative path can construct the same claims map without going through the
	 * profile behavior (the wallet's {@code configure} calls {@code setupPlainFapi}
	 * which overwrites {@code profileBehavior} to {@code PlainFAPIClientProfileBehavior},
	 * so an instance-method route would CCE).
	 */
	@Override
	public Map<String, Object> additionalSdJwtClaims() {
		return buildSdJwtStatusListClaims(module.getEnv());
	}

	public static Map<String, Object> buildSdJwtStatusListClaims(Environment env) {
		String issuer = env.getString("server", "issuer");
		String statusListUri = (issuer == null ? "" : issuer) + "statuslists/1";

		Map<Object, Object> statusListEntry = new HashMap<>();
		statusListEntry.put("idx", 0);
		statusListEntry.put("uri", statusListUri);

		Map<Object, Object> status = new HashMap<>();
		status.put("status_list", statusListEntry);

		Map<String, Object> additionalClaims = new HashMap<>();
		additionalClaims.put("status", status);
		return additionalClaims;
	}

	@Override
	public ConditionSequence additionalServerConfiguration() {
		// Init the HAIP status list scaffolding for the FAPI2SP-outer path. The wallet
		// path calls initializeStatusListState directly from its imperative configure.
		initializeStatusListState(module.getEnv());
		return super.additionalServerConfiguration();
	}

	/**
	 * Status list scaffolding shared by the wallet's imperative {@code configure} and
	 * the FAPI2SP-outer's {@link #additionalServerConfiguration} hook:
	 * <ul>
	 *   <li>Seeds {@code env.vci.status_lists.status_list_1} with an empty placeholder so
	 *       the statuslists endpoint can resolve it (and {@link VCIClientProfileBehavior}'s
	 *       statuslists serving doesn't 404). The list contents are populated lazily by
	 *       credential issuance.</li>
	 *   <li>Advertises {@code server.status_list_aggregation_endpoint} (issuer +
	 *       {@code statuslists}) so anything reading from server metadata finds the URL.</li>
	 * </ul>
	 * Called unconditionally for VCI wallet profiles (HAIP and plain), matching master:
	 * {@link VCIClientProfileBehavior}'s aggregation response always advertises
	 * {@code statuslists/1}, so the underlying placeholder needs to exist regardless of
	 * whether credentials carry a {@code status_list} claim.
	 */
	public static void initializeStatusListState(Environment env) {
		JsonObject statusLists = new JsonObject();
		statusLists.add("status_list_1", new JsonObject());
		env.putObject("vci", "status_lists", statusLists);

		String issuer = env.getString("server", "issuer");
		env.putString("server", "status_list_aggregation_endpoint", issuer + "statuslists");
	}
}
