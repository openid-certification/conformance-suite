package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.testmodule.AbstractTestModule;

public abstract class AbstractBlockLoggingTestModule extends AbstractTestModule {

	protected void runInBlock(String blockText, Runnable actor) {
		eventLog.startBlock(blockText);
		actor.run();
		eventLog.endBlock();
	}

}
