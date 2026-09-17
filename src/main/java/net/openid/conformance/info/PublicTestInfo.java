package net.openid.conformance.info;

import net.openid.conformance.testmodule.TestModule.Status;
import net.openid.conformance.variant.VariantSelection;
import org.springframework.data.annotation.Transient;

import java.util.Map;

public class PublicTestInfo implements TestListRow {

	private String _id;
	private String testId;
	private String testName;
	private VariantSelection variant;
	private String started;
	private String description;
	private String alias;
	private Map<String, String> owner;
	private String planId;
	private Status status;
	private String version;
	private String summary;
	private String publish;
	private String result;
	/** Attached to a listing row from the plan the test belongs to; never stored with the test. */
	@Transient
	private String planName;

	public String getId() {
		return _id;
	}

	public String getTestId() {
		return testId;
	}

	public String getTestName() {
		return testName;
	}

	public VariantSelection getVariant() {
		return variant;
	}

	public String getStarted() {
		return started;
	}

	public String getDescription() {
		return description;
	}

	public String getAlias() {
		return alias;
	}

	public Map<String, String> getOwner() {
		return owner;
	}

	@Override
	public String getPlanId() {
		return planId;
	}

	public String getPlanName() {
		return planName;
	}

	@Override
	public void setPlanName(String planName) {
		this.planName = planName;
	}

	public Status getStatus() {
		return status;
	}

	public String getVersion() {
		return version;
	}

	public String getSummary() {
		return summary;
	}

	public String getPublish() {
		return publish;
	}

	public String getResult() {
		return result;
	}
}
