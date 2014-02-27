package top;

import java.io.IOException;
import java.util.Iterator;

import org.junit.Test;
import org.ndexbio.rest.NdexRestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CommandProcessor {
	
	private NdexRestClient client;
	
	public CommandProcessor (){
		
	}
	
	private void performTests(){
		try {
			printTermsInNetworkByNamespace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// For the moment, as a proof of concept, 
		// we will ignore the arguments and just run
		// a pre-packaged set of queries to the local NDEx,
		// assuming that it is running with the default database.
		CommandProcessor cp = new CommandProcessor();
		cp.performTests();

	}
	
	private Iterator<JsonNode> findNetworksByProperty(String property, String value, String operator, Integer maxNetworks) throws JsonProcessingException, IOException{
		String route = "/networks/search/exact-match"; // exact-match is not relevant, but its a required part of the route
		String searchString = "[" + property + "]" + operator + "\"" + value + "\"";
		ObjectMapper mapper = new ObjectMapper();
		JsonNode searchParameters = mapper.createObjectNode(); // will be of type ObjectNode
		((ObjectNode) searchParameters).put("searchString", searchString);
		((ObjectNode) searchParameters).put("top", maxNetworks.toString());
		((ObjectNode) searchParameters).put("skip", "0");

		JsonNode response = client.post(route, searchParameters);
		Iterator<JsonNode> elements = response.elements();
		return elements;	
	}

	private Iterator<JsonNode> findTermsInNetworkByNamespace(String namespacePrefix, String networkId) throws JsonProcessingException, IOException{
		String termQueryRoute = "/networks/" + networkId + "/namespaces";
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode namespaces = mapper.createArrayNode(); // will be of type ObjectNode
		namespaces.add(namespacePrefix);
		JsonNode response = client.post(termQueryRoute, namespaces);
		Iterator<JsonNode> elements = response.elements();
		return elements;
	}
	

	public void printTermsInNetworkByNamespace() throws Exception {
		client = new NdexRestClient("dexterpratt", "insecure");
		Iterator<JsonNode> networks = findNetworksByProperty("Source", "Protein Interaction Database", "=", 10);
		while (networks.hasNext()){
			JsonNode network = networks.next();
			System.out.println("\n______\n" + network.get("name").asText() + "  id = " + network.get("id").asText() + "\nTerms:");
			
			Iterator<JsonNode> elements = findTermsInNetworkByNamespace("HGNC", network.get("id").asText());
			while (elements.hasNext()){
				JsonNode term = elements.next();
				System.out.println(" " + term.get("name").asText() + "\t  id = " + term.get("id").asText());
			}
			
		}
		
	}

}
