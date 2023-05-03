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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.unigrid.janus.model.rpc.entity.Info;
import org.unigrid.janus.model.service.RPCService;

@Data
@ApplicationScoped
public class ExternalVersion {

	@Getter
	@Setter
	private String daemonVersion = "";
	@Getter
	@Setter
	private String hedgehogVersion = "";

	@Inject private RPCService rpc;

	public void callRPCForDaemonVersion() {
		System.out.println("Getting daemon version");
		Info info = new Info();
		try {
			info = rpc.call(new Info.Request(), Info.class);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		System.out.println("Getting daemon version");
		String version = String.valueOf(info.getResult().getVersion());
		System.out.println("Getting daemon version");
		Pattern p = Pattern.compile("^(.)(..)(..)");
		System.out.println("Getting daemon version");
		Matcher m = p.matcher(version);
		System.out.println("Getting daemon version");
		m.find();
		System.out.println("Getting daemon version");
		int major = Integer.valueOf(m.group(1));
		System.out.println("Getting daemon version");
		int minor = Integer.valueOf(m.group(2));
		System.out.println("Getting daemon version");
		int revision = Integer.valueOf(m.group(3));
		System.out.println("Getting daemon version");
		String delimiter = ".";
		System.out.println("Getting daemon version");
		version = major + delimiter + minor + delimiter + revision;
		System.out.println("Getting daemon version");
		daemonVersion = version;
	}
}
