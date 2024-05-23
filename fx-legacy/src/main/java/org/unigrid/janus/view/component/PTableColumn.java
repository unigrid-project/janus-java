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

package org.unigrid.janus.view.component;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class PTableColumn<S, T> extends TableColumn<S, T> {

	private final DoubleProperty percentageWidth = new SimpleDoubleProperty(1);

	public PTableColumn() {
		tableViewProperty().addListener(new ChangeListener<TableView<S>>() {

			@Override
			public void changed(ObservableValue<? extends TableView<S>> ov, TableView<S> t, TableView<S> t1) {
				if (PTableColumn.this.prefWidthProperty().isBound()) {
					PTableColumn.this.prefWidthProperty().unbind();
				}

				PTableColumn.this.prefWidthProperty().bind(t1.widthProperty().multiply(percentageWidth));
			}
		});
	}

	public final DoubleProperty percentageWidthProperty() {
		return this.percentageWidth;
	}

	public final double getPercentageWidth() {
		return this.percentageWidthProperty().get();
	}

	public final void setPercentageWidth(double value) throws IllegalArgumentException {
		if (value >= 0 && value <= 1) {
			this.percentageWidthProperty().set(value);
		} else {
			throw new IllegalArgumentException(
				String.format("The provided percentage width is not between"
					+ " 0.0 and 1.0. Value is: %1$s", value));
		}
	}
}
