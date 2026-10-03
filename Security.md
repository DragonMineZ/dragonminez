# Security Policy

The DragonMineZ team values the efforts of the community in responsibly disclosing security vulnerabilities. We take
security issues seriously and are committed to resolving them promptly to protect our users.

## Reporting Security Issues

To report a security vulnerability:

1. **Do not disclose it publicly.** Sharing details of a vulnerability before it is resolved could put users at risk.
2. **Submit a report:**

    - Use the ["Report a Vulnerability"](https://github.com/DragonMineZ/DragonMineZ/security/advisories/new) tab on
      GitHub.
    - Alternatively, you can contact us on Discord.
    - Include the following details:
        - Steps to reproduce the issue
        - Affected versions
        - Potential impacts
        - Any suggested fixes or mitigations (if available)
3. **What to expect:**

    - We will acknowledge your report within **48 to 72 hours**.
    - Our team will investigate and provide a plan of action within **10 days** or less.
    - You will be kept updated throughout the process and credited for your discovery unless you wish to remain
      anonymous.

## Security Fix and Notification Process

After verifying and fixing a security vulnerability, we will:

- Release a patch for the affected versions listed in the **Supported Versions** table below.
- Publish a security advisory summarizing the issue, its impact, and how it was resolved.
- Notify relevant parties as necessary.

## Supported Versions

The following versions are actively supported with security updates:

| Version        | Supported |
|----------------|-----------|
| 2.x (Latest)   | ✅         |
| Older Versions | ❌         |

## Securing Your Server

A few settings matter for keeping a DragonMineZ server safe:

- **Database credentials** — if you use the `DATABASE` storage backend, replace the default `username`/`password`
  (`root`/`password`) in the `storage` section of `general-server.json`, and give DragonMineZ its own database user
  with access to its database only. See [[General Server|General-Server#storage]].
- **Permissions** — DragonMineZ commands are protected by permission nodes. Only give admin-level nodes (stat editing,
  config reloads, quest management, etc.) to trusted staff. See [[Permissions|Permissions]].
- **Backups** — back up the world folder (and your database, if you use one) before switching storage backends or
  updating the mod. See [[Notice to Server Owners/Developers|Notice-to-server-owners-developers]].
- **Official downloads only** — install DragonMineZ only from its official Modrinth and CurseForge pages.

For addon developers: never trust values sent by the client in your own packets. Validate on the server anything that
changes stats, unlocks, quests, wishes or progression (see [[Implementation & Events|Implementation-&-Events#networking]]).

Thank you for helping us maintain the security and integrity of DragonMineZ!
