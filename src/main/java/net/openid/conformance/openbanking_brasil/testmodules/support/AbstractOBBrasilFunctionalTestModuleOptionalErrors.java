package net.openid.conformance.openbanking_brasil.testmodules.support;


public abstract class AbstractOBBrasilFunctionalTestModuleOptionalErrors extends AbstractFunctionalTestModuleOptionalErrors {

	protected void runInBlock(String blockText, Runnable actor) {
		eventLog.startBlock(blockText);
		actor.run();
		eventLog.endBlock();
	}

}
