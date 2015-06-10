/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.ndexbio.sync;

import java.io.IOException;

import org.ndexbio.model.object.Permissions;

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
			sourceNetworks = this.source.ndex.findNetworks(queryString, true, queryAccountName, Permissions.ADMIN,false, 0, queryLimit);
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
