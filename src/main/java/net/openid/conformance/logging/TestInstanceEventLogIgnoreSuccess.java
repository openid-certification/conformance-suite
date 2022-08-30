package net.openid.conformance.logging;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class TestInstanceEventLogIgnoreSuccess extends TestInstanceEventLog {


	private final TestInstanceEventLog testInstanceEventLog;

	public TestInstanceEventLogIgnoreSuccess(TestInstanceEventLog testInstanceEventLog) {
		super(null, null, null);
		this.testInstanceEventLog = testInstanceEventLog;
	}

	@Override
	public synchronized String startBlock(String message) {
		return testInstanceEventLog.startBlock(message);
	}

	@Override
	public synchronized String endBlock() {
		return testInstanceEventLog.endBlock();
	}

	@Override
	public synchronized void log(String source, String msg) {
		//Not needed
	}

	@Override
	public synchronized void log(String source, JsonObject obj) {
		if (obj.has("result")) {
			String result = OIDFJSON.getString(obj.get("result"));
			if (!Strings.isNullOrEmpty(result) && !result.equals(Condition.ConditionResult.SUCCESS.toString())) {
				testInstanceEventLog.log(source, obj);
			}
		} else if (obj.has("http")) {
			testInstanceEventLog.log(source, obj);
		}

	}

	@Override
	public synchronized void log(String source, Map<String, Object> map) {
		if (source.equals("-START-BLOCK-")) {
			testInstanceEventLog.log(source, map);
		} else if (map.containsKey("result")) {
			String result = map.get("result").toString();
			if (result != null && !result.equals(Condition.ConditionResult.SUCCESS.toString())) {
				testInstanceEventLog.log(source, map);
			}
		}
	}
}
