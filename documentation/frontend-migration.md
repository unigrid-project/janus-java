# Janus frontend migration: JavaFX to embedded Chromium + server-rendered HTML

Status: Proposal. Not implemented.
Last updated: 2026-08-26

## 1. Context

Janus is a desktop wallet for the Unigrid network. The UI is JavaFX: 2,176
lines of FXML across 17 files, 273 lines of JavaFX CSS, and roughly 3,675 LOC
of controllers. 54 of the 149 production Java files import `javafx`.

Underneath the UI the architecture is already well separated:

- **Weld SE** provides CDI, with a `model.signal` package of 14 CDI event types
  acting as an application-wide signal bus.
- **Jersey JAX-RS client** speaks JSON-RPC to the local `unigridd` daemon.
  Request and response entities live in `model.rpc.entity`.
- Supporting services (`Daemon`, polling tasks, `Hedgehog`, configuration,
  update4j) hold no view code.

The business logic therefore does not depend on what draws the pixels. The
migration cost is concentrated in FXML, the controllers, and the JavaFX
property types that leaked into the model.

## 2. Goals

1. Remove the JavaFX styling ceiling. The UI must be authorable in standard
   CSS with full modern layout (grid, flexbox, container queries, transitions).
2. Restore iteration speed. Changing a screen must not require a full
   application relaunch, and the UI must be inspectable with browser developer
   tools.
3. Author the UI as hand-written HTML and CSS, with Java driving the DOM.

## 3. Non-goals

- **No JavaScript framework, no Node.js, no npm, no bundler, no build step for
  the frontend.** No React, Vue, Svelte, or equivalent. The only client-side
  JavaScript is htmx and its SSE extension, two vendored files totalling around
  16KB, used entirely through HTML attributes.
- No change to the JSON-RPC protocol itself, or to how the wallet talks to
  `unigridd`. The client that speaks it is being rewritten; the wire format is
  not.
- No change to the shape of the distribution. Native installers via jlink and
  jpackage remain the delivery mechanism, though the packaging and auto-update
  machinery is rebuilt rather than carried over.
- No redesign of the visual identity. This work reproduces the existing
  screens; restyling is separate follow-up work made possible by it.

## 4. Decision

Render the UI as HTML in an embedded Chromium browser, served by an embedded
Jetty server inside the same JVM, with pages produced by Thymeleaf templates
and interactivity supplied by htmx.

| Concern | Choice |
|---|---|
| Render shell | JCEF (Java Chromium Embedded Framework) via jcefmaven |
| Window host | Undecorated Swing `JFrame` |
| HTTP server | Embedded Jetty 12, core `Handler` API (no Servlet spec) |
| Templating | Thymeleaf 3, standalone (not thymeleaf-spring) |
| Interactivity | htmx plus its SSE extension, vendored as static files |
| Live updates | Server-Sent Events, bridged from existing CDI events |
| Business logic | Unchanged: Weld CDI, Jersey JSON-RPC client |

### Why the window host moves off JavaFX

JCEF's browser is an AWT/Swing heavyweight component. Embedding it into a
JavaFX scene requires off-screen rendering mode, and jcefmaven documents that
OSR mode is unsupported on `win-arm64`. Taking that path would drop a
supported platform and introduce input-handling complexity.

Hosting JCEF in an undecorated `JFrame` avoids this entirely, and simplifies
the codebase: the custom window chrome currently implemented by
`WindowBarController`, `WindowBorderController`, `MovableWindowDecorator` and
`ResizableWindowDecorator` becomes HTML and CSS, using the browser's own drag
regions and resize handling.

### Why Jetty rather than a custom scheme handler

A `CefSchemeHandlerFactory` would avoid opening a listening socket. It was
rejected because it forecloses the primary velocity benefit: with an HTTP
server, the entire UI can be developed in an ordinary Chrome window with
developer tools and template hot reload, without launching JCEF at all. The
security cost of a listening socket is bounded and addressed in section 8.

## 5. Architecture

```
                  single JVM process
  +---------------------------------------------------+
  |                                                    |
  |  JFrame (undecorated)                              |
  |    +-- JCEF browser  --->  http://127.0.0.1:<port> |
  |                                    |               |
  |                                    v               |
  |                        Jetty 12 (127.0.0.1 only)   |
  |                          +-- UiHandler   (pages)   |
  |                          +-- ApiHandler  (fragments)|
  |                          +-- EventHandler (SSE)    |
  |                          +-- StaticHandler (css/js)|
  |                                    |               |
  |                                    v               |
  |                        Thymeleaf TemplateEngine    |
  |                                    |               |
  |                                    v               |
  |            Weld CDI container (existing)           |
  |              services, CDI signal bus              |
  |                                    |               |
  +------------------------------------|---------------+
                                       v
                            Jersey JSON-RPC client
                                       |
                                       v
                              unigridd (local daemon)
```

### 5.1 New module layout

The JavaFX modules `fx`, `bootstrap` and `config` have been removed from
`master`. They remain available on the `legacy-javafx` branch, which is the
reference for behaviour this reimplementation must reproduce.

```
janus-java/
  core/           model, services, daemon protocol client, signals  (no UI)
  web/            Jetty, Thymeleaf, handlers, templates, css, htmx
  shell/          JFrame + JCEF host, tray, lifecycle
  desktop/        jlink/jpackage assembly; outside the reactor until
                  shell provides an entry point
```

The three-way split is what keeps the model testable without a UI toolkit at
all, and keeps the rendering concern (`web`) separable from the native window
concern (`shell`). `core` must not depend on `web` or `shell`; this is enforced
by an ArchUnit rule rather than by convention.

### 5.2 Components

**`shell`**
- `JanusFrame` — undecorated `JFrame`, sizing and positioning restored from
  `Preferences`.
- `BrowserHost` — JCEF lifecycle: `CefApp` init, `CefClient`, browser creation,
  graceful shutdown. Points the browser at the URL published by `web`.
- `TrayHost` — replaces `FXTrayIcon` with `java.awt.SystemTray`.
- `WindowCommands` — the handful of native operations HTML cannot perform:
  minimise, maximise, close, start-drag, open external URL in the system
  browser, clipboard access. Exposed to the page as HTTP endpoints, not as a
  JavaScript bridge, so there is one integration mechanism rather than two.

**`web`**
- `UiServer` — starts Jetty bound to `127.0.0.1` on an ephemeral port, returns
  the resolved URL and session token.
- `UiHandler` — full-page routes: `/wallet`, `/transactions`, `/addresses`,
  `/nodes`, `/settings`, `/documentation`, `/governance`.
- `ApiHandler` — fragment routes returning HTML partials for htmx swaps, e.g.
  `POST /api/wallet/send`, `GET /api/transactions/rows`.
- `EventHandler` — `GET /events`, a `text/event-stream` response.
- `StaticHandler` — CSS, images, fonts, `htmx.min.js`, `sse.js`.
- `SignalBridge` — CDI observer that translates existing signals
  (`NewBlock`, `State`, `WalletRequest`, `SplashMessage`, ...) into SSE frames.

Handlers hold no logic. They call existing CDI beans and hand results to
Thymeleaf. This keeps them thin enough to be read at a glance and keeps the
testable surface in `core`.

## 6. Data flow

### 6.1 User action (request/response)

1. User submits the send-funds form. The markup carries
   `hx-post="/api/wallet/send" hx-target="#send-result"`.
2. htmx issues an XHR; no page reload, no hand-written JavaScript.
3. `ApiHandler` resolves the `WalletService` CDI bean and performs the RPC.
4. The handler renders `fragments/send-result.html` through Thymeleaf.
5. htmx swaps the returned HTML into `#send-result`.

### 6.2 Live updates (server push)

1. The page opens one SSE connection: `<body hx-ext="sse" sse-connect="/events">`.
2. `SignalBridge` observes existing CDI events. On `NewBlock`, it renders the
   affected fragment and writes an SSE frame naming the event.
3. Elements subscribe by name:
   `<div id="balance" sse-swap="balance-changed">`.
4. htmx replaces the element content with the pushed HTML.

Server-Sent Events are chosen over WebSocket because every live update in this
application is one-directional, server to client. User actions travel over
ordinary requests. WebSocket would add a second protocol, a second failure
mode, and reconnection logic for no gain. SSE reconnects automatically.

### 6.3 Threading

Jetty handler threads must not block on daemon RPC. Handlers dispatch to the
existing polling/executor services and complete the response asynchronously.
The 15 current `Platform.runLater` call sites disappear: there is no UI thread
to marshal onto, because the UI is a browser rendering HTML that the server
produced.

## 7. JPMS, jlink and packaging

This is the highest-risk area of the migration and the part most likely to
require rework. The `desktop` module uses `<packaging>jlink</packaging>` and
`fx` declares a real `module-info.java`, so every dependency must be an
explicit module. **jlink cannot link automatic modules.**

### 7.1 Dependency status

Status below is observed from `mvn dependency:list` against the pinned
versions, not inferred from documentation.

| Dependency | JPMS status | Action |
|---|---|---|
| Jetty 12 (`org.eclipse.jetty.server`) | Real named module | None. Links cleanly. |
| Yasson (`org.eclipse.yasson`) | Real named module | None. |
| slf4j-api 2.x | Real named module | None. |
| Thymeleaf 3 | Automatic (`thymeleaf`) | **Must be patched.** |
| attoparser, unbescape, ognl (Thymeleaf deps) | Automatic | **Must be patched.** |
| JCEF (`jcefmaven`) | Automatic | **Must be patched.** |
| Weld SE (`weld.se.core`) | Automatic | Precedent exists — see below. |
| Jersey client (`jersey.client`) | Automatic | Precedent exists — see below. |

The precedent matters more than it first appears. Weld and Jersey were already
automatic modules in the JavaFX implementation, and that build nonetheless
produced working jlink images and installers via
`tentackle-jlink-maven-plugin`, which synthesises descriptors for non-modular
jars. So "jlink cannot link automatic modules" has already been worked around
once in this repository. The open question is narrower than it looked: not
*whether* automatic modules can be linked here, but whether the same mechanism
copes with jcefmaven's native-bearing artifact.

### 7.2 Resolution

Add the **ModiTect** Maven plugin to generate and inject `module-info.class`
into Thymeleaf and its transitive dependencies at build time. This is the
Maven equivalent of what the badass-jlink plugin does for Gradle, which is not
applicable here.

If JCEF cannot be patched this way, the fallback is to abandon `jlink`
packaging for `desktop` and build a classpath-based `jpackage` image with an
explicitly specified runtime. This produces a larger image and loses module
encapsulation, but it is a well-trodden path and does not block the migration.
That decision must be made before writing handler code, because it determines
whether `core` and `web` keep their `module-info.java` files at all.

### 7.3 Thymeleaf resource encapsulation

Under JPMS, resources inside a named module whose path maps to a valid package
name are encapsulated. `ClassLoader.getResourceAsStream` cannot see them.
Thymeleaf's default `ClassLoaderTemplateResolver` uses exactly that call, so
templates under `templates/` will not resolve inside a jlink'd image.

Two options, in order of preference:

1. Supply a custom `ITemplateResolver` that reads through
   `Class.getResourceAsStream`, which is module-local and unaffected.
2. Declare `opens templates;` and `opens static;` in `module-info.java`.

Option 1 is preferred because it keeps the module properly encapsulated rather
than opening packages to the world to work around a library's assumptions.

### 7.4 OGNL reflection

Standalone Thymeleaf evaluates expressions with OGNL, which reflects over the
objects placed in the template context. Every package whose types are exposed
to a template must be opened to OGNL:

```java
opens org.unigrid.janus.core.model to ognl;
opens org.unigrid.janus.core.rpc.entity to ognl;
```

An alternative that avoids this entirely is to expose only purpose-built view
records to templates rather than domain objects. This is worth doing on its
own merits — it keeps template expressions honest and prevents templates from
reaching arbitrarily deep into the model — and it narrows the `opens` surface
to a single package.

### 7.5 Native binaries

jcefmaven downloads the Chromium natives on first run by default. **This is
unacceptable for a wallet**: it means fetching and executing unverified code
after installation. The native artifacts must be declared as build
dependencies and bundled into the installer.

Consequences:

- One native bundle per platform, roughly 100MB each. Include exactly one per
  build; do not ship all platforms in one installer.
- Installer size grows from roughly 60–80MB to roughly 180–200MB per platform.
- Current jcefmaven releases cover linux amd64/arm64/arm, windows
  amd64/arm64/i386, and macos amd64/arm64, which matches or exceeds the
  platforms currently shipped.

### 7.6 macOS signing

Chromium ships helper executables that must each be signed with the hardened
runtime and appropriate entitlements; signing only the outer `.app` produces a
bundle that will not launch on current macOS. The release workflow at
`.github/workflows/ubuntu-deb-release.yml:254` currently references a Developer
ID certificate that is no longer valid following the copyright transfer to
Stiftelsen The Unigrid Foundation. **A replacement Developer ID certificate is
a prerequisite for macOS releases and is currently an open blocker**,
independent of this migration but made more demanding by it.

## 8. Security

The wallet is a custodial-key application, so the added attack surface must be
justified and bounded.

| Risk | Mitigation |
|---|---|
| Listening socket reachable by other local processes | Bind `127.0.0.1` only, never `0.0.0.0`. Use an ephemeral port. |
| Another local process or browser tab drives the wallet | Generate a random token per launch. The shell passes it to JCEF as a cookie; every handler rejects requests without it. |
| Cross-site request forgery from a page in the user's real browser | Reject requests carrying an `Origin` header that is not the server's own. Token check already covers this; the origin check is defence in depth. |
| Remote content loaded into the browser | Deny navigation to any non-local URL in a `CefRequestHandler`. External links open in the system browser through `WindowCommands`. |
| Injection into rendered HTML | Thymeleaf escapes by default. Never use unescaped output (`th:utext`) for daemon or user-supplied data. |
| Chromium CVE exposure | Accepted, and new. Chromium must be tracked and jcefmaven bumped on security releases. This is an ongoing maintenance commitment, not a one-time cost. |
| Developer tools exposing wallet internals | Enabled in development builds, disabled in release builds. |

The Chromium CVE treadmill is the single most significant new obligation this
migration creates. It should be an explicit, accepted decision rather than a
discovered consequence.

## 9. Migration strategy

This is a reimplementation, not a migration. There is no parallel build and no
launch flag selecting between two user interfaces: `master` carries the new
implementation only, and `legacy-javafx` is consulted as a reference for
behaviour rather than run alongside.

The consequence is that **the application is not runnable until the shell,
server and one route exist together**. That is accepted. Nothing between the
first and third stage below produces something a user can start.

**Scaffolding.** *(done)* Modules `core`, `web` and `shell` created with their
POMs, dependency versions pinned, `desktop` parked outside the reactor. No
code.

**De-risk, with a throwaway spike.** Prove JCEF links under jlink and jpackage
on one platform: a bare `JFrame` hosting JCEF showing a Thymeleaf-rendered page
served by Jetty, packaged as an installer. This answers section 7.1 and 7.2 and
should happen before handler code is written, because a forced fallback to
classpath-based `jpackage` determines whether the modules carry
`module-info.java` at all.

**Daemon protocol client.** Rebuild the JSON-RPC client and its entity types in
`core` — plain types, CDI events for signalling, no UI toolkit dependency.
`legacy-javafx` is the reference for the wire format, which is unchanged. This
stage is verifiable in isolation against a running `unigridd` without any UI.

**Shell and server skeleton.** `shell` and `web` together, serving one route end
to end. Establish the CSS foundation, the SSE bridge, and `WindowCommands`.
This is the first stage that produces something runnable.

**Screens.** Build out in ascending order of difficulty, with `legacy-javafx`
as the behavioural reference: documentation, transactions, addresses, wallet,
governance, nodes, settings.

**Packaging and updates.** Rebuild the jlink/jpackage chain around `shell`,
return `desktop` to the reactor, and rebuild the auto-update mechanism and the
release workflows. The deleted workflows on `legacy-javafx` are the starting
point.

## 10. Testing

The current suite uses jqwik, JUnit 5, jMockit, ArchUnit, and TestFX with
Monocle. TestFX disappears with JavaFX; the rest survives and improves.

| Layer | Approach |
|---|---|
| `core` logic | Existing jqwik and JUnit tests, now runnable with no UI toolkit and no Monocle headless setup. A clear net improvement. |
| Handlers | Start Jetty on an ephemeral port, issue real HTTP requests, assert on returned HTML. Fast, no browser required. |
| Template rendering | Render templates against fixed view models and assert on the output. |
| Architecture rules | Extend the existing ArchUnit tests: `core` must not depend on `web` or `shell`; handlers must not contain RPC calls. |
| End-to-end | Deferred. Driving JCEF from tests is expensive and brittle. Handler plus template tests cover the logic; visual verification stays manual until the migration settles. |

## 11. Cost

Everything below is rebuilt. Nothing carries over as code; `legacy-javafx` is
consulted, not imported.

| Item | Assessment |
|---|---|
| User interface | 2,176 lines FXML and 273 lines JavaFX CSS replaced by HTML templates and standard CSS; ~3,675 LOC of controllers replaced by handlers |
| Domain and protocol | ~5,279 LOC of model, including the Jersey JSON-RPC client and ~90 entity types, `Daemon`, polling services and `Hedgehog`, rewritten from scratch in `core`. The wire format is unchanged, so `legacy-javafx` remains an accurate specification of it. |
| Launcher and updater | `bootstrap` (JavaFX updater UI, update4j delegate, Sentry) and `config` (update4j manifest generator) removed; the auto-update mechanism is rebuilt |
| Release chain | Five packaging and release workflows removed and rebuilt once `shell` can package |
| Removed dependencies | javafx-controls, -graphics, -fxml, -media, -swing; ControlsFX; ikonli; FXTrayIcon; TestFX; Monocle; update4j; Sentry |
| Added dependencies | jcefmaven plus one native bundle; Jetty 12; Thymeleaf 3 and attoparser, unbescape, ognl; ModiTect (build only) |
| Installer size | roughly 60–80MB to roughly 180–200MB per platform |
| New ongoing burden | Tracking Chromium security releases |

The domain and protocol row is the expensive one, and it is a deliberate choice
rather than a technical necessity: that code worked. It is being rewritten to
avoid carrying the JavaFX-shaped design of the old model forward into a
codebase that no longer has a UI toolkit in it.

## 12. Open questions

1. jcefmaven is confirmed an automatic module, so it needs the same treatment
   Weld and Jersey already received. Does `tentackle-jlink-maven-plugin` handle
   an artifact that carries native binaries, or is a classpath-based `jpackage`
   fallback required? Resolved by the de-risking spike.
2. Is the roughly 2.5x installer size increase acceptable for the user base,
   given wallet users on constrained connections?
3. Is a replacement macOS Developer ID certificate for Stiftelsen The Unigrid
   Foundation obtainable, and on what timescale? Blocks macOS releases
   regardless of this work.
4. What replaces update4j? Its differential update mechanism has to cope with a
   roughly 100MB native payload that changes on every Chromium bump, and the
   generator that produced its manifests has been removed.
5. Should the governance and proposal screens be reproduced as they were, or is
   that feature set changing independently in a way that would waste the effort?

## 13. Not decided here

The visual redesign. This document covers replacing the rendering and
authoring layer while reproducing existing screens. What the wallet should
look like once standard CSS is available is a separate design exercise, and is
the actual payoff of this work.
