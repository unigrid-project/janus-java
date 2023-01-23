/*
	The Janus Wallet
	Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

	This program is free software: you can redistribute it and/or modify it under the terms of the
	addended GNU Affero General Public License as published by the Free Software Foundation, version 3
	of the License (see COPYING and COPYING.addendum).

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU Affero General Public License for more details.

	You should have received an addended copy of the GNU Affero General Public License with this program.
	If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.model;

import java.util.Queue;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeStatus {
	private static final int MAX_OUTPUT_SIZE = 65535;
	public static final double DONE = 1.0;
	public static final double INPROGRESS = -1.0;
	public static final int GRIDPANE_MAX_COLUMN = 4;
	public static final int OUTPUT_WIDTH = 300;
	public static final int OUTPUT_HEIGHT = 100;
	public static final int TOTAL_STEPS = 3;

	private String name;
	private String username;
	private String stepDescription = "";
	private String ipAddress;
	private double progress = INPROGRESS;
	private Queue<Character> output = new CircularFifoQueue<Character>(MAX_OUTPUT_SIZE);
	private boolean showOutput = false;
	private int step = 1;
	private int nodeNumber = 0;


	public void responseAddLine(String line) {
		output.addAll(line.chars().mapToObj(c -> (char) c).collect(Collectors.toList()));
	}

	public String getOutputAsString() {
		return StringUtils.join(output.toArray(), null);
	}
	public String getFullDescription() {
		return name + "[" + nodeNumber + "] " + " Steps:" + step + "/" + TOTAL_STEPS + " " + stepDescription;
	}
}
