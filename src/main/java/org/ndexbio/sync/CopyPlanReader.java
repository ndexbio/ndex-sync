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
		List<CopyPlan> copyPlans = new ArrayList<>();
		if (null == directory) {
			throw new Exception("Directory with copy plans not found");

		}
		File[] files = directory.listFiles();
		
		if (null == files) throw new Exception("Directory with copy plans not found");

		for (final File fileEntry : directory.listFiles()) {
			if (fileEntry.isFile()) {
				try {
					CopyPlan plan = objectMapper.readValue(
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
