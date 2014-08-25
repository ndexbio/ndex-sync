package top;

import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NdexServer {
	
	String username;
	String password;
	String route;
	NdexRestClient client;
	NdexRestClientModelAccessLayer ndex;
	
	public NdexServer() {
		super();

	}
	
	public NdexRestClientModelAccessLayer initialize(){
		client = new NdexRestClient(username, password, route);
		ndex = new NdexRestClientModelAccessLayer(client);
		return ndex;
	}
	
	public NdexRestClientModelAccessLayer getNdex() {
		return ndex;
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
	
	


	
	

}
