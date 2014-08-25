package top;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ndexbio.model.object.network.NetworkSummary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IdCopyPlan extends CopyPlan {
	
	List<String> idList;

	public IdCopyPlan() {
		super();
	}
	
	public void findSourceNetworks(){
		try {
			
			sourceNetworks = new ArrayList<NetworkSummary>();
			
			for (String sourceNetworkId : idList){
				NetworkSummary network = source.getNdex().getNetwork(sourceNetworkId);
				if (null != network){
					sourceNetworks.add(network);
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public List<String> getIdList() {
		return idList;
	}

	public void setIdList(List<String> idList) {
		this.idList = idList;
	}
	
	

}
