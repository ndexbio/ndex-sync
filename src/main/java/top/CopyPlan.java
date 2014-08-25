package top;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Provenance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.core.JsonProcessingException;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "planType")
@JsonSubTypes(value = { @Type(value = QueryCopyPlan.class, name = "QueryCopyPlan"), @Type(value = IdCopyPlan.class, name = "IdCopyPlan") })
public abstract class CopyPlan {
	
	NdexServer source;
	NdexServer target;

	String targetGroupName;
	String planFileName;
	List<NetworkSummary> sourceNetworks;
	List<NetworkSummary> targetCandidates;
	Map<String, Provenance> provenanceMap;
	

	public void process() throws JsonProcessingException, IOException {
		source.initialize();
		target.initialize();
		findSourceNetworks();
		//getAllSourceProvenance();
		findTargetCandidates();
		//getAllTargetProvenance();
		for (NetworkSummary network: sourceNetworks){
			processSourceNetwork(network);
		}
		
	}

	private void findTargetCandidates() throws JsonProcessingException, IOException {
		// Find networks in target NDEx in the target account.
				// ...for the moment...
				//        this is always the target user account.
				//        and the number of networks queried is limited to 100
				List<NetworkSummary> targetCandidates = target.getNdex().findNetworks("", target.getUsername(), 0, 100);
				System.out.println("Found " + targetCandidates.size() + " networks in target NDEx under  " + target.getUsername());
		
	}

	public void findSourceNetworks(){
		System.out.println("Default no-op method for finding source networks");
		sourceNetworks = new ArrayList<NetworkSummary>();
	}
	
	private void getAllTargetProvenance() throws JsonProcessingException, IOException {
		// Get the provenance structure for each candidate network and store by UUID in the provenance map
		getAllProvenance(target, targetCandidates);
	}

	private void getAllSourceProvenance() throws JsonProcessingException, IOException {
		// Get the provenance structure for each candidate network and store by UUID in the provenance map
		getAllProvenance(source, sourceNetworks);
	}
	
	private void getAllProvenance(NdexServer server, List<NetworkSummary> networks) throws JsonProcessingException, IOException{
		for (NetworkSummary network : networks){
			Provenance provenance = server.getNdex().getNetworkProvenance(network.getExternalId().toString());
			if (null != provenance){
				provenanceMap.put(network.getExternalId().toString(), provenance);
			}
		}
		
	}

	private void processSourceNetwork(NetworkSummary sourceNetwork) throws JsonProcessingException, IOException {
		System.out.println("Processing source network " + sourceNetwork.getName() + " last modified " + sourceNetwork.getModificationDate());
		//Provenance sourceProvenance = provenanceMap.get(sourceNetwork.getExternalId().toString());
		
		
		
		// for each, get provenance and determine whether it is a first generation copy of the source network
		
	}
	
	// Attributes to be read from file

	public String getTargetGroupName() {
		return targetGroupName;
	}

	public void setTargetAccountName(String targetGroupName) {
		this.targetGroupName = targetGroupName;
	}

	public String getPlanFileName() {
		return planFileName;
	}

	public void setPlanFileName(String planFileName) {
		this.planFileName = planFileName;
	}

	public NdexServer getSource() {
		return source;
	}

	public void setSource(NdexServer source) {
		this.source = source;
	}

	public NdexServer getTarget() {
		return target;
	}

	public void setTarget(NdexServer target) {
		this.target = target;
	}


}
