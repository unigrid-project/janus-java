/*
    The Janus Wallet
    Copyright © 2021-2022 Stiftelsen The Unigrid Foundation

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.web;

import java.util.Map;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class Templates {
	private static final String PREFIX = "templates/";
	private static final String SUFFIX = ".html";

	private final TemplateEngine engine = new TemplateEngine();

	public Templates(final boolean cache) {
		final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();

		resolver.setPrefix(PREFIX);
		resolver.setSuffix(SUFFIX);
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setCharacterEncoding("UTF-8");

		/* Development switches caching off so an edited template appears on the next
		   request, which is the entire point of building the interface in a browser. */
		resolver.setCacheable(cache);
		engine.setTemplateResolver(resolver);
	}

	public String render(final String template, final Map<String, Object> variables) {
		final Context context = new Context();

		context.setVariables(variables);
		return engine.process(template, context);
	}
}
