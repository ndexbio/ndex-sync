package org.ndexbio.sync;

import java.io.IOException;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryCopyPlan extends CopyPlan {
	
	String queryAccountName;
	String queryString;
	int queryLimit;



	public QueryCopyPlan() {
		super();
	}



	@Override
	public void findSourceNetworks(){
		System.out.println("finding up to " + queryLimit 
				+ " source networks by query '" + queryString + "' with accountName " + queryAccountName);
		try {
			sourceNetworks = this.source.ndex.findNetworks(queryString, queryAccountName, 0, queryLimit);
			LOGGER.info("Found " + sourceNetworks.size() + " networks");
		} catch (IOException e) {
			LOGGER.severe("Error while finding source networks: " + e.getMessage());
			e.printStackTrace();
		}
		
	}



	public String getQueryAccountName() {
		return queryAccountName;
	}



	public void setQueryAccountName(String accountName) {
		this.queryAccountName = accountName;
	}



	public String getQueryString() {
		return queryString;
	}



	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}



	public int getQueryLimit() {
		return queryLimit;
	}



	public void setQueryLimit(int limit) {
		this.queryLimit = limit;
	}
	
	

}
