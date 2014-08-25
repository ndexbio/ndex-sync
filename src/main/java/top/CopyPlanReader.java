package top;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CopyPlanReader {
	ObjectMapper objectMapper = null;
	File directory = null;

	public CopyPlanReader(String directoryPathString) {
		super();
		directory = new File(directoryPathString);
		objectMapper = new ObjectMapper();
	}

	public List<CopyPlan> getCopyPlans() throws Exception {
		List<CopyPlan> copyPlans = new ArrayList<CopyPlan>();
		if (null == directory) {
			throw new Exception("Directory with copy plans not found");

		} else {
			
			File[] files = directory.listFiles();
			
			if (null == files) throw new Exception("Directory with copy plans not found");

			for (final File fileEntry : directory.listFiles()) {
				if (fileEntry.isFile()) {
					try {
						CopyPlan plan = (CopyPlan) objectMapper.readValue(
								fileEntry, CopyPlan.class);
						plan.setPlanFileName(fileEntry.getName());
						copyPlans.add(plan);
					} catch (Exception e) {
						System.out.println("Error parsing : "
								+ fileEntry.getName() + " ->  "
								+ e.getMessage());

					}
				} else {
					System.out.println("Skipping: " + fileEntry.getName());
				}
			}
			return copyPlans;
		}

	}

}
