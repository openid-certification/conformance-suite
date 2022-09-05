package net.openid.conformance.condition.client;

public class FAPIBrazilOpenBankingCheckDirectoryApiBase extends AbstractFAPIBrazilCheckDirectoryApiBase {

	@Override
	protected String getErrorMessage() {
		return "Testing for Brazil certification must be done using the Brazil directory. If you do not have access to the directory an example client is available in the conformance suite instructions.";
	}

	@Override
	String getExpectedUrl() {
		return "https://matls-api.sandbox.directory.openbankingbrasil.org.br/";
	}

}
