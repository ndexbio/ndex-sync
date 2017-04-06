/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.sync;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.ProvenanceEntity;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NdexServer {
	
	String username;
	String password;
	String route;
	NdexRestClient client;
	NdexRestClientModelAccessLayer ndex;
	
	String version;
	
	public NdexServer() {
		super();

	}
	
	public NdexRestClientModelAccessLayer initialize() throws JsonProcessingException, IOException, NdexException{
		client = new NdexRestClient(username, password, route);
		ndex = new NdexRestClientModelAccessLayer(client);
		
		Object o = client.getNdexObject("/admin", "/status", Object.class);
		if ( o == null)
			throw new NdexException("Failed to get status on server endpoint " + this.route);
		if ( o instanceof Map) {
			Map<String,String> props =(Map<String,String>) ((Map<String,Object>)o).get("properties");
			String v = props.get("ServerVersion");
			if (v!=null) 
				version = v;
			else
				version = "1.3";
		} else 
		  throw new NdexException ("Failed to get version of ndex server. Status object returned was: " + o.toString());
		return ndex;
	}
	
	public NdexRestClientModelAccessLayer getNdex() {
		return ndex;
	}
	
	
	public String getVersion() throws JsonProcessingException, IOException, NdexException {
		return version;
	}
	//____________________________________
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
	}
	
	
	public String getHostName () throws URISyntaxException { 
		URI uri = new URI(route);
	    return uri.getHost();	
	}
	
	public boolean finishedLoading(UUID networkId) throws JsonProcessingException, IOException, NdexException {
		NetworkSummary s = ndex.getNetworkSummaryById(networkId.toString());
		return s.getIsValid();
	}

	public boolean isReadOnly(UUID networkId) throws JsonProcessingException, IOException, NdexException {
		NetworkSummary s = ndex.getNetworkSummaryById(networkId.toString());
        return s.getIsReadOnly();
	/*	Map<String, Object> summaryMap = getNetworkSummaryAsMap(networkId);	
		Boolean b = (Boolean)summaryMap.get("isReadOnly");
		return b.booleanValue(); */
	}
	
/*	private Map<String,Object> getNetworkSummaryAsMap(UUID networkId) throws JsonProcessingException, IOException, NdexException {
		if (! version.equals("2.0"))
			throw new NdexException("This function only support NDEx 2.0 server.");
		Object o =client.getNdexObject("/network", "/"+networkId.toString() + "/summary", Object.class);
		if ( o == null || !(o instanceof Map)) 
			throw new NdexException ("Can't get summary of network " + networkId + " on server " + route);
		Map<String,Object> result = (Map<String,Object>)o;
		if ( result.get("errorMessage") !=null )
			throw new NdexException ("Target NDEx server failed to validate network " + networkId);
		return result;
	} */

	public void setNetworkProvenance(UUID networkId, ProvenanceEntity newProvananceHistory) throws IllegalStateException, Exception {
		int counter = 0;
		while (counter < 30) {
			try {
				ndex.setNetworkProvenance(networkId.toString(), newProvananceHistory);
				return ;
			} catch (IOException e) {
				System.out.println("Failed to set provenance: " + e.getMessage() + ". Retry in 3 seconds...");
				e.printStackTrace();
				Thread.sleep(3000);
				counter ++;
			}	
		}
		throw new NdexException("Set provenance function timed out after 30 retries.");
	}

}
