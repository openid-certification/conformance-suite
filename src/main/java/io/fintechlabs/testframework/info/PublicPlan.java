package io.fintechlabs.testframework.info;

import java.util.List;

import io.fintechlabs.testframework.variant.VariantSelection;

public class PublicPlan {

	private String _id;
	private String planName;
	private VariantSelection variant;
	private String description;
	private String started;
	private List<Plan.Module> modules;
	private String publish;
	private String version;

	public String getId() {
		return _id;
	}

	public String getPlanName() {
		return planName;
	}

	public VariantSelection getVariant() {
		return variant;
	}

	public String getDescription() {
		return description;
	}

	public String getStarted() {
		return started;
	}

	public List<Plan.Module> getModules() {
		return modules;
	}

	public String getPublish() {
		return publish;
	}

	public String getVersion() {
		return version;
	}
}
