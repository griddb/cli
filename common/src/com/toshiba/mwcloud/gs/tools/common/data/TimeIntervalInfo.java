package com.toshiba.mwcloud.gs.tools.common.data;

public class TimeIntervalInfo {
	
	private String containerFile;
	
	private String boundaryValue;

	public TimeIntervalInfo(String containerFile, String boundaryValue) {
		this.setContainerFile(containerFile);
		this.setBoundaryValue(boundaryValue);
	}	
	
	public String getContainerFile() {
		return containerFile;
	}
	
	public void setContainerFile(String containerFile) {
		this.containerFile = containerFile;
	}
	
	public String getBoundaryValue() {
		return boundaryValue;
	}
	
	public void setBoundaryValue(String boundaryValue) {
		this.boundaryValue = boundaryValue;
	}
	
}