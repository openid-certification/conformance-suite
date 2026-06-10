package net.openid.conformance.vci10wallet;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VCI1FinalCredentialFormat;
import net.openid.conformance.vci10wallet.condition.VCIEnsureProofCountWithinAdvertisedBatchSize;
import net.openid.conformance.vci10wallet.condition.VCIEnsureProofKeysAreDistinct;
import net.openid.conformance.vci10wallet.condition.VCIReverseCredentialIssuanceOrder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@PublishTestModule(
	testName = "oid4vci-1_0-wallet-test-batch-credential-issuance",
	displayName = "OID4VCI 1.0: Wallet Test Batch Credential Issuance",
	summary = """
		This test case validates that a wallet can obtain a batch of credentials from an emulated issuer, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) 1.0 specification.\

		The emulated issuer advertises 'batch_credential_issuance' with a batch_size of 10 in its metadata. The wallet is expected to send multiple key proofs in the 'proofs' parameter of the credential request; one credential is issued per proof key, all containing the same Credential Dataset but bound to the wallet's different keys.\

		The issued credentials are deliberately returned in the REVERSE order of the proofs in the request: the specification defines no correspondence between the order of the credentials array and the order of the proofs, so the wallet must identify the binding key from each credential itself rather than relying on the response order.\

		The test verifies the wallet does not send more proofs than the advertised batch_size and that the proof keys are pairwise distinct (required for SD-JWT credentials so verifiers cannot link the credentials; recommended for mdoc).\

		If the wallet only requests a single credential (one proof), the test is skipped after the credential has been delivered, as batch behavior cannot be evaluated.

		Note that if the vci_grant_type=pre_authorization_code is used, you can use 123456 as the tx_code.
		""",
	profile = "OID4VCI-1_0"
)
public class VCIWalletTestBatchCredentialIssuance extends AbstractVCIWalletTest {

	protected static final int ADVERTISED_BATCH_SIZE = 10;

	/** Skip-on-single-credential grace period; just long enough for the HTTP 200 to reach the wallet. */
	private static final int SKIP_DELAY_SECONDS = 5;

	/** Set once a delivery contained two or more credentials - the skip timer then stands down. */
	private final AtomicBoolean batchDelivered = new AtomicBoolean(false);

	/** One-shot guard so only the first single-credential delivery schedules the skip timer. */
	private final AtomicBoolean skipScheduled = new AtomicBoolean(false);

	@Override
	protected Integer getAdvertisedBatchSize() {
		return ADVERTISED_BATCH_SIZE;
	}

	@Override
	protected void createCredential() {
		// proof_jwts is only present for the 'jwt' proof type; for attestation proofs (one
		// attestation attesting several keys) these request-level checks don't apply.
		skipIfMissing(new String[]{"proof_jwts"}, null, ConditionResult.INFO,
			VCIEnsureProofCountWithinAdvertisedBatchSize.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-12.2.4");

		if (vciCredentialFormat == VCI1FinalCredentialFormat.SD_JWT_VC) {
			skipIfMissing(new String[]{"proof_jwts"}, null, ConditionResult.INFO,
				VCIEnsureProofKeysAreDistinct.class, ConditionResult.FAILURE, "SDJWT-10.1");
		} else {
			skipIfMissing(new String[]{"proof_jwts"}, null, ConditionResult.INFO,
				VCIEnsureProofKeysAreDistinct.class, ConditionResult.WARNING, "OID4VCI-1FINAL-3.3.2");
		}

		super.createCredential();

		callAndStopOnFailure(VCIReverseCredentialIssuanceOrder.class, "OID4VCI-1FINAL-8.3");
	}

	@Override
	protected void onCredentialSent() {
		JsonObject credentialIssuance = env.getObject("credential_issuance");
		int credentialCount = credentialIssuance != null
			? credentialIssuance.getAsJsonArray("credentials").size()
			: 0;

		if (credentialCount >= 2) {
			batchDelivered.set(true);
			super.onCredentialSent();
			return;
		}

		// A single proof means batch behavior cannot be evaluated. The skip must not be
		// fired from this thread - this hook runs before the HTTP response is returned to
		// the wallet - so schedule it in the background after a short grace period (the
		// same pattern as VCIWalletTestCredentialIssuanceWithNotification).
		setStatus(Status.WAITING);

		if (!skipScheduled.compareAndSet(false, true)) {
			return;
		}

		eventLog.log(getName(), "The wallet only requested a single credential; skipping the test in "
			+ SKIP_DELAY_SECONDS + " seconds unless a batch request arrives first.");

		getTestExecutionManager().scheduleInBackground(() -> {
			if (!batchDelivered.get() && getStatus() == Status.WAITING) {
				setStatus(Status.RUNNING);
				fireTestSkipped("The wallet only sent one proof in the credential request, so batch "
					+ "issuance behavior (multiple credentials returned in a different order) cannot "
					+ "be evaluated.");
			}
			return null;
		}, SKIP_DELAY_SECONDS, TimeUnit.SECONDS);
	}
}
