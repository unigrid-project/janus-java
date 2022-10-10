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

package org.unigrid.updatewalletconfig;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

public class ConsoleDependencyGraphDumper
	implements DependencyVisitor {

	private final PrintStream out;

	private final List<ChildInfo> childInfos = new ArrayList<>();

	public ConsoleDependencyGraphDumper() {
		this(null);
	}

	public ConsoleDependencyGraphDumper(PrintStream out) {
		this.out = (out != null) ? out : System.out;
	}

	public boolean visitEnter(DependencyNode node) {
		out.println(formatIndentation() + formatNode(node));
		childInfos.add(new ChildInfo(node.getChildren().size()));
		return true;
	}

	private String formatIndentation() {
		StringBuilder buffer = new StringBuilder(128);
		for (Iterator<ChildInfo> it = childInfos.iterator(); it.hasNext();) {
			buffer.append(it.next().formatIndentation(!it.hasNext()));
		}
		return buffer.toString();
	}

	private String formatNode(DependencyNode node) {
		StringBuilder buffer = new StringBuilder(128);
		Artifact a = node.getArtifact();
		Dependency d = node.getDependency();
		buffer.append(a);
		if (d != null && d.getScope().length() > 0) {
			buffer.append(" [").append(d.getScope());
			if (d.isOptional()) {
				buffer.append(", optional");
			}
			buffer.append("]");
		}
		String premanaged = DependencyManagerUtils.getPremanagedVersion(node);
		if (premanaged != null && !premanaged.equals(a.getBaseVersion())) {
			buffer.append(" (version managed from ").append(premanaged).append(")");
		}

		premanaged = DependencyManagerUtils.getPremanagedScope(node);
		if (premanaged != null && !premanaged.equals(d.getScope())) {
			buffer.append(" (scope managed from ").append(premanaged).append(")");
		}
		DependencyNode winner = (DependencyNode) node.getData().get(ConflictResolver.NODE_DATA_WINNER);
		if (winner != null && !ArtifactIdUtils.equalsId(a, winner.getArtifact())) {
			Artifact w = winner.getArtifact();
			buffer.append(" (conflicts with ");
			if (ArtifactIdUtils.toVersionlessId(a).equals(ArtifactIdUtils.toVersionlessId(w))) {
				buffer.append(w.getVersion());
			} else {
				buffer.append(w);
			}
			buffer.append(")");
		}
		return buffer.toString();
	}

	public boolean visitLeave(DependencyNode node) {
		if (!childInfos.isEmpty()) {
			childInfos.remove(childInfos.size() - 1);
		}
		if (!childInfos.isEmpty()) {
			childInfos.get(childInfos.size() - 1).index++;
		}
		return true;
	}

	private static class ChildInfo {

		final int count;

		int index;

		ChildInfo(int count) {
			this.count = count;
		}

		public String formatIndentation(boolean end) {
			boolean last = index + 1 >= count;
			if (end) {
				return last ? "\\- " : "+- ";
			}
			return last ? "   " : "|  ";
		}

	}

}
