package net.openid.conformance.export;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class PlanExportInfo {
	private Date exportedAt;
	private String exportedFrom;
	private Map<String, String> exportedBy;
	private String exportedVersion;
	private Object planInfo;
	private List<TestExportInfoHolder> testLogExports;

	public PlanExportInfo(String baseUrl, Map<String, String> principal, String version, Object planInfo) {
		this.exportedAt = new Date();
		this.exportedFrom = baseUrl;
		this.exportedBy = principal;
		this.exportedVersion = version;
		this.planInfo = planInfo;
		testLogExports = new ArrayList<>();
	}

	public void addTestExportInfoHolder(TestExportInfoHolder testExportInfoHolder) {
		this.testLogExports.add(testExportInfoHolder);
	}

	public int getTestExportCount() {
		return testLogExports.size();
	}

	public Date getExportedAt() {
		return exportedAt;
	}

	public void setExportedAt(Date exportedAt) {
		this.exportedAt = exportedAt;
	}

	public String getExportedFrom() {
		return exportedFrom;
	}

	public void setExportedFrom(String exportedFrom) {
		this.exportedFrom = exportedFrom;
	}

	public Map<String, String> getExportedBy() {
		return exportedBy;
	}

	public void setExportedBy(Map<String, String> exportedBy) {
		this.exportedBy = exportedBy;
	}

	public String getExportedVersion() {
		return exportedVersion;
	}

	public void setExportedVersion(String exportedVersion) {
		this.exportedVersion = exportedVersion;
	}

	public Object getPlanInfo() {
		return planInfo;
	}

	public void setPlanInfo(Object planInfo) {
		this.planInfo = planInfo;
	}

	public List<TestExportInfoHolder> getTestLogExports() {
		return testLogExports;
	}

	public static class TestExportInfoHolder{
		private String testId;
		private String testModuleName;
		private TestExportInfo export;

		public TestExportInfoHolder(String testId, String testModuleName, TestExportInfo testExportInfo) {
			this.testId = testId;
			this.testModuleName = testModuleName;
			this.export = testExportInfo;
		}

		public String getTestId() {
			return testId;
		}

		public void setTestId(String testId) {
			this.testId = testId;
		}

		public String getTestModuleName() {
			return testModuleName;
		}

		public void setTestModuleName(String testModuleName) {
			this.testModuleName = testModuleName;
		}

		public TestExportInfo getExport() {
			return export;
		}

		public void setExport(TestExportInfo export) {
			this.export = export;
		}
	}
}
