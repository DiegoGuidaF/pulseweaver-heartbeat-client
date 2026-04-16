# Security Policy

## Reporting a Vulnerability

Please **do not** open a public GitHub issue for security vulnerabilities.

Use GitHub's [private vulnerability reporting](https://github.com/DiegoGuidaF/pulseweaver-heartbeat-client/security/advisories/new)
to submit a report. You will receive a response within 7 days, and a fix or mitigation plan within 30 days
for confirmed vulnerabilities.

Include as much of the following as possible:

- Description of the vulnerability and its potential impact
- Steps to reproduce or proof-of-concept
- Affected versions and platforms (Android, desktop)
- Any suggested mitigations you are aware of

## Supported Versions

Only the latest release is actively maintained.

## Scope

This policy covers the PulseWeaver Heartbeat Client app. For the PulseWeaver server, please report via the
[PulseWeaver repository](https://github.com/DiegoGuidaF/PulseWeaver).

The following are in scope:

- Credential or API key exposure (storage, logging, transmission)
- Bypass of biometric authentication on Android
- Network traffic interception or MITM due to incorrect TLS handling
- Unauthorized access to stored configuration

The following are **not** in scope:

- Vulnerabilities that require physical access to an already-unlocked device
- CVEs in transitive dependencies where the vulnerable code path is not reachable from this app
  (e.g. server-side HTTP attack vectors in libraries used only as an HTTP client)
