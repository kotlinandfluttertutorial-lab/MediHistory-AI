# MediHistory AI — Release Readiness Checklist

> This checklist must be completed before any production release. Items marked **BLOCKS release** must be resolved before the build is published to the Play Store or any external environment.

---

## Pre-release Checklist

### Functional Verification (all must pass)

- [ ] All 82 acceptance criteria verified against implemented screens
- [ ] 13 screens manually tested with synthetic data
- [ ] OCR review state machine tested: PENDING_REVIEW → REVIEWED → PENDING_REVIEW (un-review)
- [ ] Duplicate detection tested: paracetamol/acetaminophen synonym detected
- [ ] Medical history disclaimer visible and non-dismissible
- [ ] New Doctor Mode consent dialog shown before every share
- [ ] Reminders opt-in verified: no reminder auto-created

---

### Security Sign-off (BLOCKS release if any incomplete)

- [ ] **SQLCipher evaluation completed** (see EncryptionDecisionDoc.kt) — CURRENTLY DEFERRED
- [ ] Cross-patient access returns 404 in production environment (not just tests)
- [ ] All tokens and credentials stored in EncryptedSharedPreferences — confirmed via code audit
- [ ] No API keys in any committed file (CI secrets scan passes)
- [ ] Rate limiting configured for production scale (current: 100 req/60s — review for production)
- [ ] HTTPS enforced for all backend calls
- [ ] Android network security config reviewed

---

### Privacy Review (BLOCKS release if any incomplete)

- [ ] Privacy policy reviewed by qualified legal counsel
- [ ] Data retention policy documented and communicated to users
- [ ] Export and deletion flows tested end-to-end
- [ ] Cloud backup retention limitations explained to users (see PrivacyScreen)
- [ ] Consent mechanisms reviewed for applicable India requirements
- [ ] No prescription text, medicine details, or tokens in any logs (production log audit)

---

### Medical Safety Review (BLOCKS release if any incomplete)

- [ ] All "potential duplicate" labels reviewed — none use clinical terms
- [ ] Medical history disclaimer text reviewed by medical professional
- [ ] OCR confidence thresholds reviewed for clinical appropriateness
- [ ] No fabricated medical data in any code path (audit review)
- [ ] AI-generated labels present on all AI summary sections

---

### Accessibility (BLOCKS release)

- [ ] TalkBack audit completed on all 13 screens with a real Android device
- [ ] All interactive elements have minimum 48×48 dp touch targets (AccessibilityChecks passing)
- [ ] All status indicators use both colour AND text (StatusBadge, ConfidenceBadge verified)
- [ ] Font size scaling tested at 200% on all screens

---

### Technical Sign-off

- [ ] All open Dependabot PRs reviewed and actioned
- [ ] No SNAPSHOT or beta dependencies in production build
- [ ] proguard-rules.pro reviewed and tested
- [ ] App signed with production keystore (not debug)
- [ ] Backend deployed with production secrets (not .env defaults)
- [ ] Database migration from v1 to current version tested
- [ ] SQLCipher migration path documented if encryption is added post-launch

---

### Figma and Documentation

- [ ] All [FIGMA_NODE_TBD] placeholders resolved in docs/design/screen-inventory.md
- [ ] docs/ARTIFACT-COMPLETION-REPORT.md updated with final file count
- [ ] All 82 Jira tickets at Done status
- [ ] Traceability matrix updated

---

## Known Blockers (must resolve before launch)

1. **SQLCipher evaluation** — documented in EncryptionDecisionDoc.kt
2. **Legal/security review** — India data protection requirements
3. **TalkBack audit** — requires real device testing
4. **Production secrets** — JWT_SECRET_KEY and DATABASE_URL not yet provisioned
5. **Figma node IDs** — FIGMA_NODE_TBD placeholders in screen-inventory.md

---

## Post-launch Monitoring

- No prescription text or medicine details in server logs
- No tokens in Android Logcat
- Error rates for OCR processing and duplicate detection
- User-reported false positives in duplicate alerts
