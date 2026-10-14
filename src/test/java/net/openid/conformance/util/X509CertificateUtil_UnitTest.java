package net.openid.conformance.util;

import net.openid.conformance.condition.client.lote.LoteTestFixtures;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The chain validation against a set of trust anchors: any anchor of the set may terminate the
 * path, the one that did is returned, and no anchor may appear in the chain.
 */
public class X509CertificateUtil_UnitTest {

	private final LoteTestFixtures.ListedCa caA = LoteTestFixtures.INSTANCE.generateCa("CA A", "Org");
	private final LoteTestFixtures.ListedCa caB = LoteTestFixtures.INSTANCE.generateCa("CA B", "Org");
	private final X509Certificate leafUnderB = LoteTestFixtures.INSTANCE.mintLeaf(caB, "leaf").getSecond();

	@Test
	public void aChainToAnyAnchorOfTheSetPassesAndTheMatchedAnchorIsReturned() throws Exception {
		X509Certificate matched = X509CertificateUtil.validateX5cCertificateChain(List.of(leafUnderB), List.of(caA.getCert(), caB.getCert()));

		assertThat(matched).isEqualTo(caB.getCert());
	}

	@Test
	public void aChainToNoAnchorOfTheSetFails() {
		assertThatThrownBy(() -> X509CertificateUtil.validateX5cCertificateChain(List.of(leafUnderB), List.of(caA.getCert())))
			.isInstanceOf(X509CertificateUtil.X5cCertificateChainException.class)
			.hasMessageContaining("PKIX path validation failed");
	}

	@Test
	public void anAnchorOfTheSetInsideTheChainFails() {
		assertThatThrownBy(() -> X509CertificateUtil.validateX5cCertificateChain(List.of(leafUnderB, caB.getCert()), List.of(caA.getCert(), caB.getCert())))
			.isInstanceOf(X509CertificateUtil.X5cCertificateChainException.class)
			.hasMessageContaining("must not be included");
	}

	@Test
	public void aChainEndingAtAListedSubordinateCaPassesWhenItsRootIsListedToo() throws Exception {
		LoteTestFixtures.ListedCa root = LoteTestFixtures.INSTANCE.generateCa("Root", "Org", 1);
		LoteTestFixtures.ListedCa subCa = LoteTestFixtures.INSTANCE.generateSubCa(root, "Sub CA");
		X509Certificate leafUnderSubCa = LoteTestFixtures.INSTANCE.mintLeaf(subCa, "leaf under sub CA").getSecond();

		X509Certificate matched = X509CertificateUtil.validateX5cCertificateChain(List.of(leafUnderSubCa, subCa.getCert()),
			List.of(subCa.getCert(), root.getCert()));

		assertThat(matched).isEqualTo(root.getCert());
	}

	@Test
	public void aChainEndingAtAListedSubordinateCaFailsWhenOnlyThatCaIsListed() {
		LoteTestFixtures.ListedCa root = LoteTestFixtures.INSTANCE.generateCa("Root", "Org", 1);
		LoteTestFixtures.ListedCa subCa = LoteTestFixtures.INSTANCE.generateSubCa(root, "Sub CA");
		X509Certificate leafUnderSubCa = LoteTestFixtures.INSTANCE.mintLeaf(subCa, "leaf under sub CA").getSecond();

		assertThatThrownBy(() -> X509CertificateUtil.validateX5cCertificateChain(List.of(leafUnderSubCa, subCa.getCert()), List.of(subCa.getCert())))
			.isInstanceOf(X509CertificateUtil.X5cCertificateChainException.class)
			.hasMessageContaining("must not be included");
	}

	@Test
	public void aListedLeafIsReportedAsSuch() {
		assertThatThrownBy(() -> X509CertificateUtil.validateX5cCertificateChain(List.of(leafUnderB), List.of(leafUnderB)))
			.isInstanceOf(X509CertificateUtil.X5cCertificateChainException.class)
			.hasMessageContaining("leaf certificate in the x5c chain is itself one of the trust anchors");
	}

	@Test
	public void twoAnchorsWithTheSameSubjectAreToldApartByKey() throws Exception {
		LoteTestFixtures.ListedCa renewed = LoteTestFixtures.INSTANCE.generateCa("CA B", "Org");
		assertThat(renewed.getCert().getSubjectX500Principal()).isEqualTo(caB.getCert().getSubjectX500Principal());

		X509Certificate matched = X509CertificateUtil.validateX5cCertificateChain(List.of(leafUnderB), List.of(renewed.getCert(), caB.getCert()));

		assertThat(matched).isEqualTo(caB.getCert());
	}

	@Test
	public void anEmptySetMeansTheLegacyWalkAndReturnsNull() throws Exception {
		assertThat(X509CertificateUtil.validateX5cCertificateChain(List.of(leafUnderB), List.of())).isNull();
	}

	@Test
	public void theSingleAnchorOverloadStillWorks() throws Exception {
		X509CertificateUtil.validateX5cCertificateChain(List.of(leafUnderB), caB.getCert());
		assertThatThrownBy(() -> X509CertificateUtil.validateX5cCertificateChain(List.of(leafUnderB), caA.getCert()))
			.isInstanceOf(X509CertificateUtil.X5cCertificateChainException.class);
	}
}
