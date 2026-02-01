# Security Policy

**Reporting URL:** https://github.com/scalefirstai/openrudder/blob/main/SECURITY.md  
**Contact Email:** security@openrudder.io

OpenRudder takes security seriously. This document describes our security policy and how to report vulnerabilities.

## Supported Versions

We release patches for security vulnerabilities. Which versions are eligible for receiving such patches depends on the CVSS v3.0 Rating:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

The OpenRudder team takes security bugs seriously. We appreciate your efforts to responsibly disclose your findings, and will make every effort to acknowledge your contributions.

### How to Report a Security Vulnerability?

**⚠️ IMPORTANT: Please do not report security vulnerabilities through public GitHub issues.**

Security vulnerabilities must be reported privately to protect users. We provide two methods for private reporting:

#### Method 1: Email (Recommended)

**Email**: security@openrudder.io

Send your vulnerability report to this email address. Your report will be:
- ✅ Kept completely private and confidential
- ✅ Only shared with the security team
- ✅ Not disclosed publicly until a fix is available

**Response Time**: You will receive an initial response within **14 days** (typically within 48 hours).

#### Method 2: GitHub Security Advisories (Private)

**URL**: https://github.com/scalefirstai/openrudder/security/advisories/new

Use GitHub's private security advisory feature to report vulnerabilities confidentially. This keeps the report private until we publish a fix.

**Response Time**: You will receive an initial response within **14 days** (typically within 48 hours).

Please include the following information in your report:

- Type of issue (e.g., buffer overflow, SQL injection, cross-site scripting, etc.)
- Full paths of source file(s) related to the manifestation of the issue
- The location of the affected source code (tag/branch/commit or direct URL)
- Any special configuration required to reproduce the issue
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue, including how an attacker might exploit the issue

This information will help us triage your report more quickly.

## Preferred Languages

We prefer all communications to be in English.

## Response Time Commitment

**Initial Response Time**: We commit to providing an initial response to all vulnerability reports within **14 days** of receipt.

- **Target**: Respond within 48 hours
- **Maximum**: 14 days
- **Applies to**: All vulnerability reports received in the last 6 months
- **What counts as response**: Acknowledgment, questions for clarification, or initial assessment

If you do not receive a response within 14 days, please:
1. Send a follow-up email to security@openrudder.io
2. Contact us via our [Discord security channel](https://discord.gg/openrudder)

## Security Update Policy

When we receive a security bug report, we will:

1. Confirm the problem and determine the affected versions
2. Audit code to find any potential similar problems
3. Prepare fixes for all releases still under maintenance
4. Release new security fix versions as soon as possible

## Security Advisories

Security advisories will be published on:
- [GitHub Security Advisories](https://github.com/scalefirstai/openrudder/security/advisories)
- Our website at [openrudder.io/security](https://openrudder.io/security)
- Our Discord community

## Bug Bounty Program

We currently do not have a bug bounty program in place. However, we deeply appreciate security researchers who help us maintain the security of OpenRudder and will publicly acknowledge your contributions (with your permission).

## Security Best Practices for Users

When using OpenRudder in production:

1. **Keep Dependencies Updated**: Regularly update OpenRudder and all dependencies
2. **Secure Database Credentials**: Never hardcode credentials; use environment variables or secret management systems
3. **Network Security**: Use TLS/SSL for all network communications
4. **Access Control**: Implement proper authentication and authorization
5. **Monitoring**: Enable security monitoring and logging
6. **Regular Audits**: Conduct regular security audits of your deployment

## Security-Related Configuration

### Secure Database Connections

```yaml
openrudder:
  sources:
    postgres:
      ssl-mode: require
      ssl-cert: /path/to/cert
      ssl-key: /path/to/key
```

### API Key Management

```java
// ✅ Good - Use environment variables
String apiKey = System.getenv("OPENAI_API_KEY");

// ❌ Bad - Never hardcode
String apiKey = "sk-..."; 
```

## Acknowledgments

We would like to thank the following security researchers for responsibly disclosing vulnerabilities:

- *Your name could be here!*

## Contact

For any security-related questions or concerns, please contact:
- **Email**: security@openrudder.io
- **PGP Key**: Available at [openrudder.io/pgp](https://openrudder.io/pgp)

---

**Thank you for helping keep OpenRudder and our users safe!**
