# Security Policy

## Reporting a vulnerability

**Please do not open a public issue for security problems.**

Use GitHub's private reporting instead:

1. Go to the **Security** tab of this repository
2. Click **Report a vulnerability**
3. Describe the issue with steps to reproduce

If private reporting is unavailable, contact the maintainer through the GitHub profile
[`@aliaoxxx`](https://github.com/aliaoxxx).

Please include:

- The affected version or commit
- What an attacker can achieve (read data / escalate privileges / DoS / …)
- Reproduction steps or a proof of concept
- Whether the issue also affects the live demo

## What to expect

- Acknowledgement within a few days
- A fix in a patch release once confirmed, with credit unless you prefer otherwise
- Public disclosure coordinated with you after the fix ships

## Scope

In scope:

- Authentication and session handling (JWT, refresh rotation, Redis whitelist)
- Authorization: RBAC, row-level data scope, endpoint permissions
- SQL injection, XSS, SSRF, path traversal
- Secrets leaking through logs, responses or exports

Out of scope:

- Issues that require an already-compromised admin account
- Findings from automated scanners without a demonstrated impact
- Denial of service through deliberately huge payloads without a rate-limit bypass
- The default demo credentials (`admin / admin123`) — they are documented and must be changed
  in any real deployment

## Supported versions

The `main` branch receives fixes. This project is pre-1.0, so there are no maintenance
branches for old versions.

---

## 中文说明

发现安全问题时**不要**开公开 issue，请用仓库 **Security** 标签页里的
**Report a vulnerability** 私密上报，或通过 GitHub 主页联系
[`@aliaoxxx`](https://github.com/aliaoxxx)。

上报时请说明：受影响的版本、攻击者能达成什么、复现步骤，以及是否影响线上演示环境。

范围说明：默认演示账号 `admin / admin123` 属于已知设计（任何正式部署都必须修改），
不作为漏洞处理。
