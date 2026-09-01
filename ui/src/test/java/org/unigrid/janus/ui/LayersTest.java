/*
    The Janus Wallet
    Copyright © 2021-2026 Stiftelsen The Unigrid Foundation

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.ui;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import net.jqwik.api.Example;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Which module may know about which, so the split survives more than the first commit. */
public class LayersTest {
	private static final JavaClasses CLASSES = new ClassFileImporter()
		.withImportOption(new ImportOption.DoNotIncludeTests())
		.importPackages("org.unigrid.janus");

	@Example
	public void shouldKeepViewsFreeOfTheModelAndTheServer() {
		noClasses().that().resideInAPackage("..janus.ui.view..")
			.should().dependOnClassesThat().resideInAnyPackage("..janus.core..", "org.eclipse.jetty..")
			.check(CLASSES);
	}

	@Example
	public void shouldKeepTheInterfaceOffTheServer() {
		noClasses().that().resideInAPackage("..janus.ui..")
			.should().dependOnClassesThat().resideInAPackage("org.eclipse.jetty..")
			.check(CLASSES);
	}

	@Example
	public void shouldKeepTheModelUnawareOfTheInterface() {
		noClasses().that().resideInAPackage("..janus.core..")
			.should().dependOnClassesThat().resideInAnyPackage("..janus.web..", "..janus.ui..")
			.check(CLASSES);
	}
}
