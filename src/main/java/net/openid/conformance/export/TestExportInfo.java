package net.openid.conformance.export;

import java.util.Date;
import java.util.List;
import java.util.Map;
@SuppressWarnings("rawtypes")
public class TestExportInfo {
	private Date exportedAt;
	private String exportedFrom;
	private Map<String, String> exportedBy;
	private String exportedVersion;
	private Object testInfo;
	private List results;

	public TestExportInfo(String baseUrl, Map<String, String> principal, String version, Object testInfo, List testResults) {
		this.exportedAt = new Date();
		this.exportedFrom = baseUrl;
		this.exportedBy = principal;
		this.exportedVersion = version;
		this.testInfo = testInfo;
		this.results = testResults;
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

	public Object getTestInfo() {
		return testInfo;
	}

	public void setTestInfo(Object testInfo) {
		this.testInfo = testInfo;
	}

	public List getResults() {
		return results;
	}

	public void setResults(List results) {
		this.results = results;
	}
}
