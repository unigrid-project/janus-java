package org.unigrid.janus;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CycleTest extends ArchiTectureTest {
	@Test @Disabled // TODO: Fix and enable
	public void shouldNotHaveCircularDependencies() {
		slices().matching("org.unigrid.janus.(*)..").should().beFreeOfCycles().check(getClasses());
	}

}
