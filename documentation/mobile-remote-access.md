# Remote wallet access from mobile devices

Status: proposal, October 2026. Touches Janus, the Hedgehog gridnodes and DNS run by the Unigrid Foundation.

## Goal
A user views their Janus wallet from a phone, on iOS and Android alike, and drops files from the phone into network
storage. Nothing is installed on the phone and nothing goes through an app store: the phone uses its browser. Keys,
wallet logic and page rendering all stay in the desktop Janus.

Out of scope:
- The phone acting as a gridnode or a full node.
- Spending keys on the phone.
- Access while the desktop is switched off or asleep.

## Why the wallet does not run on the phone
- The QUIC transport depends on `netty-incubator-codec-native-quic`, which ships native libraries for desktop Linux,
  macOS and Windows only. The Netty 4.2 codec that replaces it publishes the same set. Neither has an Android or iOS
  build.
- Weld SE, HK2 and Jersey generate JVM bytecode at run time, and `org.reflections` scans jar files. Android runs DEX
  and has no jars; iOS forbids generated code altogether.
- iOS allows no child processes, so Janus cannot start Hedgehog there, and it suspends an app shortly after it leaves
  the screen. Android limits long-running work to foreground services with time caps.
- Phones sit behind carrier NAT on metered, battery-powered links, so they cannot serve storage fragments or stay
  online for repair.

## Overview
```
Phone browser
    |  https://<label>.<domain>, TCP 443
    v
Gridnode relay          reads the TLS server name, decrypts nothing
    |  QUIC stream over the tunnel
    v
Hedgehog daemon         on the desktop
    |  TCP to 127.0.0.1
    v
Janus remote connector  TLS ends here
```

1. The user turns on remote access in Janus. Janus gets a hostname and a certificate for it.
2. The desktop Hedgehog keeps an outbound QUIC connection to a gridnode that acts as relay and registers the hostname
   there.
3. The phone opens `https://<label>.<domain>`. DNS points at the relay, which reads the server name from the TLS
   ClientHello and forwards the raw bytes over the tunnel. TLS ends inside Janus, so the relay sees ciphertext only.
4. The phone logs in with a passkey that was registered by scanning a QR code shown on the desktop.

## Components

### Wallet identity and hostname
- On first use Janus creates a dedicated tunnel key pair. It is never a key that controls funds.
- The hostname label is the base32 encoding of the first 160 bits of the SHA-256 hash of the tunnel public key, under
  a domain the Foundation controls: `<label>.<domain>`.

### Certificates
- A phone browser cannot be told to trust a single self-signed certificate, so every wallet needs a certificate from a
  public CA for its own hostname.
- Janus requests it from Let's Encrypt over ACME with acme4j, using the DNS-01 challenge. A small Foundation service
  publishes the `_acme-challenge.<label>.<domain>` TXT record for any request signed by the tunnel key whose hash
  matches the label. The certificate's private key never leaves the desktop.
- Janus renews the certificate automatically, well before it expires.
- Let's Encrypt allows about 50 new certificates per registered domain per week. The domain needs a raised limit or a
  Public Suffix List entry.
- Every issued certificate lands in the public Certificate Transparency logs, so every wallet hostname is public and
  will be scanned. Nothing may rely on the hostname being hard to guess.

### Relay on gridnodes
- Gridnode operators opt in to the relay role, which adds a public TCP listener on port 443.
- The desktop Hedgehog registers its label with a relay by signing the label, the relay's identity and a timestamp
  with the tunnel key.
- For every incoming TCP connection the relay reads the server name with Netty's `AbstractSniHandler` without
  terminating TLS, opens a QUIC stream on the matching tunnel and copies bytes both ways. An unknown name closes the
  connection.
- The DNS record for a label points at the relay that holds its tunnel. Hedgehog updates it through the Foundation
  service, with a signed request, whenever it moves to another relay.
- Each label has a bandwidth quota and a limit on concurrent connections.
- The relay sees the label, the phone's IP address, timing and volume, but no content.

### Tunnel endpoint in the desktop Hedgehog
- Hedgehog owns the network side. Janus asks it over the existing local REST interface to open a tunnel for its label
  and to forward to Janus's remote connector on `127.0.0.1`.
- Hedgehog copies each QUIC stream to a new local TCP connection. It sees ciphertext only.
- It reconnects with backoff and registers again whenever it changes relay.

### Remote interface in Janus
- `UiServer` listens on loopback only, on purpose: the wallet holds keys, so its interface must never be reachable
  from off the machine. That connector stays as it is and keeps serving the desktop window.
- A second Jetty connector, also bound to loopback, serves the remote interface over TLS with the ACME certificate,
  through its own handler and routes.
- Routes: enrollment, login, dashboard (balances, history, stored files) and upload. Wallet creation and import, seed
  and key views, backup and export, and settings are not served remotely. Sending funds is an open question.
- Sessions use their own cookie (`Secure`, `HttpOnly`, `SameSite=Strict`) with a short idle timeout. Every request
  that changes state carries a CSRF token. Responses set a strict Content Security Policy and HSTS.
- Remote access is off by default. Janus shows when it is on and which devices are registered.

### Pairing and login
- The "Add phone" dialog in Janus creates a one-time token that expires after five minutes, or when the dialog
  closes, and shows a QR code for `https://<label>.<domain>/enroll#<token>`. The token travels in the fragment so it
  never reaches an access log; the page script posts it.
- The phone registers a WebAuthn passkey with the hostname as relying party and user verification required. Janus
  shows the new device on the desktop and activates it only once the user confirms there.
- Logins use the passkey only. There are no passwords and no fallback.
- Removing a device in Janus deletes its credential and ends its sessions.
- webauthn4j and Yubico's java-webauthn-server both cover the server side.

### File upload
- Hedgehog's `POST /storage` takes the whole file in one request with an announced length of up to 64 GiB and cannot
  resume. Phones drop connections often.
- The remote interface therefore accepts chunked, resumable uploads, either the tus protocol or fixed-size chunks
  with offsets. Janus stages the chunks on the desktop disk, streams the finished file to `/storage` and records the
  returned fingerprint in the wallet's file list.
- The phone picks files with `<input type="file" multiple>`. Uploads run only while the page is open.

### Mobile pages
- The remote routes get responsive Thymeleaf templates on the same htmx base as the desktop pages.
- A web app manifest lets the user add the wallet to the home screen.
- A service worker caches the static parts and shows a "wallet unreachable" page when the tunnel is down. The relay
  cannot show one, because it holds no certificate for the hostname.

## Security
- **Relay operator:** cannot decrypt; sees metadata only.
- **Internet scanners:** hostnames are public. Only passkeys log in, login and enrollment are rate limited, and
  enrollment accepts a token only while the dialog is open on the desktop.
- **Foundation DNS and certificate service:** whoever controls the DNS zone could obtain a certificate for a wallet
  hostname and intercept its traffic. The Foundation is trusted not to, and Janus watches the Certificate
  Transparency logs for certificates on its hostname that it did not request, and warns the user.
- **Stolen phone:** the passkey needs the device unlocked and the user verified. The device is removed from the
  desktop.
- **Other processes on the desktop:** the remote connector is on loopback but still requires a passkey session.
- **Hijacked remote session:** cannot export keys or the seed, and cannot spend unless that is added with a
  confirmation on the desktop.

## Limitations
- The desktop must be running, awake and online.
- A browser gives no background uploads and no push notifications.
- Throughput is bounded by the relay and its quota.

## Alternatives considered
- **Hedgehog and Janus on the phone:** rejected, see above.
- **TLS-terminating tunnels such as ngrok or Cloudflare Tunnel in their default setup:** the relay would see
  balances and session cookies.
- **Native companion app that trusts the desktop key from the QR code:** needs no DNS or certificate service and
  gains background uploads and push notifications, at the cost of two app store reviews and a separate codebase. It
  could be added later on top of the same remote interface.
- **LAN-only access without a relay:** simpler, but useless away from home, and the browser still needs a public
  certificate.

## Prior art
- Home Assistant remote access through Nabu Casa's SniTun: end-to-end TLS through a relay that routes by server
  name.
- Tailscale Funnel: relays forward by server name and TLS ends on the user's node.
- Plex `plex.direct`: per-server certificates for servers that users host themselves.

## Open questions
- Should the phone be able to send funds, and if so with a confirmation on the desktop?
- One DNS record per wallet pointing at its relay, or a wildcard to every relay with forwarding between gridnodes?
- Who carries relay bandwidth, how operators opt in, and how quotas are set.
- The domain, and whether to list it on the Public Suffix List.
- Rotating the tunnel key, which changes the hostname and requires registering the devices again.

## Work by component
- **Foundation infrastructure:** the DNS zone and the service for ACME challenges and DNS updates.
- **Hedgehog:** the relay role on gridnodes (server-name routing, registration, quotas) and the tunnel endpoint in
  the desktop daemon, opened and closed over REST.
- **Janus:** the tunnel key and hostname, the ACME client, the remote connector and its routes, passkey enrollment and
  login, chunked upload, the mobile templates with manifest and service worker, and the settings to manage it all.
