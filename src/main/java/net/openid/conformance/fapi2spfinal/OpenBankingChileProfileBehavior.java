package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.client.AddFormBasedClientSecretToRequest;
import net.openid.conformance.condition.client.CMFChileCallDirectorySoftwareStatementEndpointWithBearerToken;
import net.openid.conformance.condition.client.ExtractDirectoryClientSecret;
import net.openid.conformance.condition.client.ExtractDirectoryConfiguration;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.function.Supplier;

/**
 * Profile behavior for Chile Open Finance.
 * Differs from plain FAPI in requiring mTLS everywhere, and in obtaining the software statement for
 * dynamic client registration from the CMF Directorio.
 */
public class OpenBankingChileProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public Supplier<? extends ConditionSequence> getDirectoryConfigurationSteps() {
		return DirectoryConfigurationSteps::new;
	}

	@Override
	public Supplier<? extends ConditionSequence> getDirectoryTokenRequestSteps() {
		return DirectoryTokenRequestSteps::new;
	}

	@Override
	public Supplier<? extends ConditionSequence> getSoftwareStatementRequestSteps() {
		return SoftwareStatementRequestSteps::new;
	}

	public static class DirectoryConfigurationSteps extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(ExtractDirectoryConfiguration.class);
			callAndStopOnFailure(ExtractDirectoryClientSecret.class);
		}
	}

	/**
	 * The CMF Directorio authenticates the suite with the directory client secret (client_secret_post).
	 */
	public static class DirectoryTokenRequestSteps extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddFormBasedClientSecretToRequest.class);
		}
	}

	public static class SoftwareStatementRequestSteps extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(CMFChileCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		}
	}
}
