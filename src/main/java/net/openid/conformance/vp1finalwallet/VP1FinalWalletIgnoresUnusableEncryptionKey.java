package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.AddUnusableEncryptionKeyToClientMetadata;
import net.openid.conformance.condition.client.AddVP1FinalEncryptionParametersToClientMetadata;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-ignores-unusable-encryption-key",
	displayName = "OID4VP-1.0-FINAL: Wallet ignores an unusable encryption key in client_metadata",
	summary = "Advertises two additional, unusable encryption keys alongside the usable key in the "
		+ "verifier's client_metadata: a post-quantum-shaped key (kty=AKP with a non-existent ML-KEM "
		+ "parameter set) and a key with a made-up key type. A conformant wallet must ignore the keys it "
		+ "cannot use and encrypt its response to the usable key, as per RFC 7517 section 5 (a recipient "
		+ "SHOULD ignore keys whose values are out of the supported ranges). This is the steady state "
		+ "during any algorithm transition - e.g. a verifier adding post-quantum keys before all wallets "
		+ "support them. The test passes if the suite can decrypt the wallet's response using the usable "
		+ "key.",
	profile = "OID4VP-1FINAL"
)
// only meaningful for encrypted response modes; the unencrypted modes never advertise an encryption key
@VariantNotApplicable(parameter = VP1FinalWalletResponseMode.class, values = {"direct_post", "dc_api"})
public class VP1FinalWalletIgnoresUnusableEncryptionKey extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.insertAfter(AddVP1FinalEncryptionParametersToClientMetadata.class,
				condition(AddUnusableEncryptionKeyToClientMetadata.class));
	}
}
