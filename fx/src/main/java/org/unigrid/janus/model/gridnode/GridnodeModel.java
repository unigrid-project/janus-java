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

package org.unigrid.janus.model.gridnode;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@ApplicationScoped
public class GridnodeModel {
    private List<GridnodeData> gridnodeDataList = new ArrayList<>();

    @Getter @Setter
    private String currentGridnodeId;

    @PostConstruct
    public void init() {
        // Initialize or reset gridnodeDataList if needed
        gridnodeDataList.clear();
    }

    public void addGridnodeData(GridnodeData data) {
        gridnodeDataList.add(data);
    }

    public List<GridnodeData> getGridnodeData() {
        return gridnodeDataList;
    }

    public void startGridnode() {
        // Implementation to start gridnode
    }
}

