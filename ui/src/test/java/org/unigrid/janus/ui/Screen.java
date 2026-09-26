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

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.unigrid.janus.web.Client;

/**
 * The page as the window holds it, changed the way htmx would change it when a control is used.
 * Only the part of htmx the templates rely on is played out, and a control asking for anything more
 * is refused, so that a test can never pass on behaviour the real page would not have.
 */
public final class Screen {
	private static final Set<String> UNDERSTOOD = Set.of("hx-post", "hx-target", "hx-swap", "hx-trigger", "hx-vals");
	private static final String OUTER = "outerHTML";
	private static final String INNER = "innerHTML";
	private static final String CLICK = "click";

	private final Client client;
	private final Document document;
	private int status;

	private Screen(final Client client, final HttpResponse<String> response) {
		this.client = client;
		this.document = Jsoup.parse(response.body());
		this.status = response.statusCode();
	}

	/** The page the shell opens, entered with the token as the window enters it. */
	public static Screen open(final ControlCenterRig rig) throws Exception {
		final Client client = new Client(rig.base());

		return new Screen(client, client.get(rig.entrance().toString()));
	}

	public Client client() {
		return client;
	}

	public Document document() {
		return document;
	}

	/** The status of the last answer, which htmx leaves on screen unswapped unless it is a 200. */
	public int status() {
		return status;
	}

	public Element find(final String css) {
		final Element element = document.selectFirst(css);

		if (element == null) {
			throw new AssertionError("Nothing on screen matches " + css + " in\n" + document.body());
		}

		return element;
	}

	public Screen click(final String css) throws Exception {
		final Element element = find(css);
		final String trigger = element.hasAttr("hx-trigger") ? element.attr("hx-trigger") : CLICK;

		if (!CLICK.equals(trigger)) {
			throw new AssertionError(css + " is not sent by a click but by " + trigger);
		}

		if (element.hasAttr("hx-vals")) {
			throw new UnsupportedOperationException(css + " works out values of its own; trigger it with them");
		}

		return send(element, Map.of());
	}

	/**
	 * Sends a control the way its own trigger would, with the values its script would have worked
	 * out; those are handed in, since no script runs here.
	 */
	public Screen trigger(final String css, final Map<String, String> values) throws Exception {
		return send(find(css), values);
	}

	private Screen send(final Element element, final Map<String, String> values) throws Exception {
		refuseWhatIsNotPlayedOut(element);

		final Element target = target(element);
		final HttpResponse<String> response = client.submit(element.attr("hx-post"), encode(values));

		status = response.statusCode();

		if (status == 200) {
			swap(target, swapStyle(element), response.body());
		}

		return this;
	}

	private static void refuseWhatIsNotPlayedOut(final Element element) {
		if (!element.hasAttr("hx-post")) {
			throw new AssertionError("Nothing is sent by " + element.cssSelector());
		}

		if (element.hasAttr("name") || element.closest("form") != null) {
			throw new UnsupportedOperationException("Values carried by forms are not played out");
		}

		for (Element carrier = element; carrier != null; carrier = carrier.parent()) {
			for (final Attribute attribute : carrier.attributes()) {
				if (attribute.getKey().startsWith("hx-") && !UNDERSTOOD.contains(attribute.getKey())) {
					throw new UnsupportedOperationException(attribute.getKey() + " is not played out");
				}
			}
		}
	}

	/* htmx looks for a target up through the ancestors, so a card can name itself once for every
	   control inside it. */
	private static Element target(final Element element) {
		final Element carrier = element.closest("[hx-target]");

		if (carrier == null) {
			return element;
		}

		if (!"this".equals(carrier.attr("hx-target"))) {
			throw new UnsupportedOperationException("Only hx-target=\"this\" is played out");
		}

		return carrier;
	}

	private static String swapStyle(final Element element) {
		final Element carrier = element.closest("[hx-swap]");

		return carrier == null ? INNER : carrier.attr("hx-swap");
	}

	private static void swap(final Element target, final String style, final String html) {
		if (!OUTER.equals(style)) {
			throw new UnsupportedOperationException("Only hx-swap=\"outerHTML\" is played out, not " + style);
		}

		for (final Element arrived : Jsoup.parseBodyFragment(html).body().children()) {
			target.before(arrived.clone());
		}

		target.remove();
	}

	private static String encode(final Map<String, String> values) {
		return values.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
			+ "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)
		).collect(Collectors.joining("&"));
	}
}
