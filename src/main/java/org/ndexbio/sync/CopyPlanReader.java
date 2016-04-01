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
			if (fileEntry.isFile() && fileEntry.getName().endsWith("json")) {
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
