# Figma Interaction Specification — MediHistory AI

**Version:** 1.0  
**Last updated:** 2025  
**Figma file:** https://www.figma.com/design/xLPxdsIl7CBJOJTtliHKj5/MediHis  
**Figma prototype page:** Page 18 — `18 — Prototype Links`

---

## 1. Navigation Flows

### 1.1 Bottom Navigation

The bottom navigation bar is the primary navigation pattern. It is persistent across all top-level destinations.

**Destinations (left to right):**

| Tab | Screen ID | Icon | Label |
|---|---|---|---|
| 1 | SCR-01-DASHBOARD | `dashboard` | Dashboard |
| 2 | SCR-02-RX-LIST | `description` | Prescriptions |
| 3 | SCR-04-MED-EXPLORER | `medication` | Medicines |
| 4 | SCR-06-HISTORY | `history_edu` | History |
| 5 | SCR-09-SAFETY-CENTER | `health_and_safety` | Safety |

**Bottom nav behavior:**
- Tapping a tab navigates to that destination's top level. If already on that destination, scroll to top.
- Tapping the current tab does not pop the back stack — it scrolls to top only.
- Tapping a different tab replaces the current destination in the back stack (save & restore state per tab).
- The back button on a top-level destination exits the app (does not traverse tabs).

**Compose implementation note:**  
Use `NavController` with `saveState = true` and `restoreState = true` per tab. See Android Navigation documentation for multi-back-stack setup.

### 1.2 Back Stack Behavior

| Flow | Back stack behavior |
|---|---|
| Dashboard → Rx List → Rx Detail | Up button or system back pops to Rx List, then Dashboard |
| Dashboard → Rx Detail (deep link) | Up button goes to Rx List (reconstructed back stack) |
| Rx List → OCR Review | Back returns to Rx List |
| Med Explorer → Med Detail | Back returns to Med Explorer |
| History → New Doctor Mode → PDF Preview | Back from PDF Preview returns to New Doctor Mode; back from New Doctor returns to History |
| Safety Center → Medicine Detail | Back returns to Safety Center |
| Any screen → Consent Dialog | Dialog dismiss returns to originating screen |
| Any screen → Settings | Back returns to originating screen |

### 1.3 Deep Links

| Trigger | Deep link pattern | Destination | Back stack |
|---|---|---|---|
| Reminder notification | `medihistory://reminder/{reminderId}` | SCR-08-REMINDERS | Dashboard → Reminders |
| Duplicate alert notification | `medihistory://duplicate/{alertId}` | SCR-05-DUPLICATE-REVIEW | Dashboard → Safety → Duplicate Review |
| New prescription added | `medihistory://prescription/{prescriptionId}` | SCR-02-RX-DETAIL | Dashboard → Rx List → Rx Detail |
| Share intent received | `medihistory://import` | SCR-03-OCR-REVIEW | Dashboard → Rx List → OCR Review |

**Deep link back stack reconstruction:**  
All deep links must reconstruct a synthetic back stack so the user can press back and arrive at a sensible parent screen (not exit the app).

---

## 2. Screen Transitions

### 2.1 Standard Transitions

All screen-to-screen transitions use Material 3 motion specs.

| Transition type | Easing | Duration | Usage |
|---|---|---|---|
| Forward (push) | `EmphasizedDecelerateEasing` | 300 ms | Navigating deeper into hierarchy |
| Back (pop) | `EmphasizedAccelerateEasing` | 250 ms | Back navigation |
| Lateral (tab switch) | `EmphasizedEasing` fade | 200 ms | Bottom nav tab switch |
| Modal enter (dialog, sheet) | `EmphasizedDecelerateEasing` slide-up | 300 ms | Dialogs, bottom sheets |
| Modal exit | `EmphasizedAccelerateEasing` slide-down | 250 ms | Dialog/sheet dismiss |

### 2.2 Shared Element Transitions

Shared element transitions communicate spatial continuity between list and detail screens.

| Transition | Shared element | From | To |
|---|---|---|---|
| Prescription → Detail | `PrescriptionCard` thumbnail image | SCR-02-RX-LIST | SCR-02-RX-DETAIL |
| Medicine → Detail | `MedicineCard` container | SCR-04-MED-EXPLORER | SCR-04-MED-DETAIL |
| Duplicate alert → Review | `DuplicateAlertCard` container | SCR-01-DASHBOARD alert banner | SCR-05-DUPLICATE-REVIEW |

**Implementation note:**  
Use `Modifier.sharedElement()` with `SharedTransitionScope` (Compose 1.7+). The shared element key must match between the list item and the detail screen. Fall back to a standard cross-fade if the shared element is not visible in the list (e.g. scrolled off screen).

**Figma representation:**  
On Page 17 (Flows & Nav Map), show the transition with a connector labeled "Shared element: card thumbnail". On the screen pages, annotate the shared element with a note: "Shared transition element — see figma-interaction-spec.md".

---

## 3. Gestures

### 3.1 Swipe to Dismiss

| Component | Swipe direction | Result | Undo available? |
|---|---|---|---|
| DuplicateAlertCard (on SCR-05) | Swipe right | Dismiss alert (mark as dismissed) | Yes — snackbar with "Undo" (5 seconds) |
| ReminderRow (on SCR-08) | Swipe left | Delete reminder | Yes — snackbar with "Undo" (5 seconds) |
| PrescriptionCard (on SCR-02) | Swipe left | Reveal "Delete" action button | N/A (not a direct delete) |

**Dismiss behavior:**
- Swipe reveals a colored background (`color/error-container`) with a delete/dismiss icon
- On release past 40% threshold: animate card out, show snackbar
- On release below 40% threshold: spring back to original position
- Undo snackbar: appears at bottom of screen, "Dismissed" + "Undo" action button, auto-dismisses after 5 seconds

**Accessibility note:**  
Swipe-to-dismiss must not be the only way to perform these actions. A long-press context menu or an overflow menu on the card must offer the same action for users who cannot perform swipe gestures.

### 3.2 Pull to Refresh

Available on list screens that show server-synchronized data.

| Screen | Pull-to-refresh behavior |
|---|---|
| SCR-02-RX-LIST | Triggers prescription sync |
| SCR-04-MED-EXPLORER | Triggers medicine data refresh |
| SCR-05-DUPLICATE-REVIEW | Triggers duplicate detection re-run |
| SCR-08-REMINDERS | Triggers reminder sync |

**Visual:** Material 3 `PullToRefreshContainer` with `CircularProgressIndicator` centered.  
**Offline behavior:** If offline, pull-to-refresh shows an offline banner (see Section 6) and does not spin indefinitely.

### 3.3 Long-Press Actions

| Component | Long-press result |
|---|---|
| `ConfidenceBadge` | Show tooltip explaining confidence level and how to review |
| `PrescriptionCard` | Show context menu: "View detail", "Delete", "Share" |
| `MedicineCard` | Show context menu: "View detail", "View source prescription" |
| `SourceLinkChip` | Show tooltip with full document name and date |

---

## 4. Micro-interactions

### 4.1 OCR Progress Indicator

The OCR extraction process has distinct visual phases:

| Phase | Visual | Duration | Notes |
|---|---|---|---|
| Upload in progress | LinearProgressIndicator (indeterminate) at top | Until upload completes | Show "Uploading…" label |
| OCR processing | CircularProgressIndicator (indeterminate) centered | Until OCR completes | Show "Extracting text…" label |
| Structured extraction | Animated list item reveal (staggered, 50 ms delay per field) | ~500 ms total | Each extracted field fades in |
| Review required | OcrStatusChip transitions to "Needs review" with amber pulse (once) | 300 ms pulse | One-time attention animation |
| Complete | OcrStatusChip transitions to "Extracted" with check_circle | 200 ms cross-fade | |
| Failed | OcrStatusChip transitions to "Extraction failed" with error icon | 200 ms | Show retry option |

**Reduced motion:** Replace all animations with instant state changes. The chip still updates to the correct state.

### 4.2 Confidence Badge Tooltip

| Trigger | Behavior |
|---|---|
| Long-press on `ConfidenceBadge` | Show tooltip with explanatory text |
| Tooltip text (High) | "This field was extracted with high confidence. Review before use." |
| Tooltip text (Medium) | "This field may contain errors. Please verify against the original document." |
| Tooltip text (Low) | "This field could not be reliably extracted. Manual review required." |
| Tooltip text (Unreviewed) | "This field has not been reviewed yet." |
| Dismiss | Tap elsewhere or wait 3 seconds |

**Accessibility:** On TalkBack, the long-press trigger is available via accessibility actions. The tooltip text is also read as the extended description of the badge.

### 4.3 Source Link Ripple

`SourceLinkChip` uses a custom ripple:
- Color: `color/primary` at 12% opacity
- Radius: chip bounds (not unlimited)
- Duration: 300 ms

This is the default Material ripple behavior. No custom implementation required unless the tonal color needs adjustment.

### 4.4 Duplicate Alert Count Badge Animation

When a new duplicate is detected:
1. The Safety tab badge count increments with a scale animation (100 ms, spring easing)
2. The Dashboard duplicate alert card appears with a slide-down enter animation (300 ms)
3. Screen reader announces: "New potential duplicate ingredient alert. [N] alerts now active."

---

## 5. Consent Dialog Flow

### 5.1 Trigger Conditions

| Action | Consent dialog type | Trigger |
|---|---|---|
| Share records (New Doctor Mode) | `Share` | User taps "Share" in SCR-07-NEW-DOCTOR |
| Export medical history | `Export` | User taps "Export" in SCR-06-HISTORY |
| Delete a single prescription | `DeleteRecord` | User confirms delete from context menu |
| Delete all data | `DeleteAllData` | User initiates from Settings → Privacy |
| Enable cloud backup | `CloudBackupEnable` | User toggles cloud backup in Settings |

### 5.2 Share Consent Flow

```
User taps "Share" button (SCR-07-NEW-DOCTOR)
  → SCR-07-PDF-PREVIEW opens showing selected records preview
  → User reviews content
  → User taps "Share PDF"
  → ConsentDialog (type=Share) appears
       Title: "Share your medical summary?"
       Body: "You are about to share [N] records including prescriptions from [doctor names]."
       Privacy note: "Only the generated PDF will be shared. Original documents are not included."
       Buttons: [Cancel] [Share]
  → User taps "Share"
  → Android system share sheet opens
  → ConsentDialog dismisses
  → Success snackbar: "Summary shared"
```

**Cancellation:** At any point, Cancel returns the user to the previous screen without sharing.

### 5.3 Delete All Data Flow

```
User navigates to Settings → Privacy → Delete all data
  → ConsentDialog (type=DeleteAllData) appears
       Title: "Delete all your data?"
       Body: "This permanently deletes all prescriptions, medicines, and extracted data. This cannot be undone."
       Privacy note: "If cloud backup is enabled, data will also be deleted from backup on next sync."
       Buttons: [Cancel] [Delete all data] (Delete button uses error color)
  → User taps "Delete all data"
  → Second confirmation: Text entry "DELETE" required
  → User types "DELETE"
  → Deletion proceeds with progress indicator
  → App returns to onboarding/empty state
```

### 5.4 Cancellation Rules

- Every consent dialog must have a visible Cancel button as the first focusable action
- Tapping outside the dialog on a scrim dismisses and cancels (same as Cancel)
- Back button/gesture dismisses and cancels
- No auto-acceptance or timeout-based acceptance

---

## 6. Error Recovery

### 6.1 Retry Patterns

| Error type | Screen | Recovery pattern |
|---|---|---|
| Network error (list load) | Any list screen | ErrorState with "Try again" button; pull-to-refresh also works |
| OCR extraction failed | SCR-03-OCR-REVIEW | OcrStatusChip → "Extraction failed" + "Retry extraction" button |
| Sync failed | Any screen | Offline/sync error banner (dismissible), retry on next app open or manual |
| PDF generation failed | SCR-07-PDF-PREVIEW | ErrorState with "Generate again" button |
| Medicine interaction check unavailable | SCR-09-SAFETY-CENTER | `Source Unavailable` state — no retry (feature unavailable) |

### 6.2 Offline Banner Behavior

The offline banner appears at the top of the screen content area (below TopAppBar, above the list) when the app cannot reach the server.

| State | Banner appearance |
|---|---|
| Offline | `SafetyBanner(severity=Warning)`, message: "You're offline. Changes will sync when connected." |
| Back online | Banner dismisses with fade animation (300 ms) |
| Sync in progress | `SafetyBanner(severity=Info)`, message: "Syncing…" with inline progress indicator |
| Sync failed | `SafetyBanner(severity=Error)`, message: "Sync failed. [Retry] [Dismiss]" |

**Behavior rules:**
- The offline banner is never dismissible by the user (it reflects a live state)
- The sync error banner is dismissible; if dismissed, it does not reappear until the next sync attempt
- Local data remains accessible in all offline states

### 6.3 Snackbar Patterns

| Trigger | Snackbar message | Action |
|---|---|---|
| Alert dismissed | "Alert dismissed" | "Undo" |
| Reminder deleted | "Reminder deleted" | "Undo" |
| Prescription deleted | "Prescription deleted" | "Undo" |
| Export complete | "Export saved to [location]" | "Open" |
| Share complete | "Summary shared" | None |
| Sync complete | "Records updated" | None |

Snackbars appear at the bottom of the screen, above the navigation bar, and auto-dismiss after 4 seconds.

---

## 7. OcrStatusChip State Machine

The OCR status chip follows this state machine. The chip is never interactive unless the state is `Reviewing` or `PartiallyExtracted`.

```
                  ┌─────────────┐
                  │   Pending   │ (initial state on upload)
                  └──────┬──────┘
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
   ┌────────────┐  ┌───────────────┐  ┌────────┐
   │ Extracted  │  │  Partially    │  │ Failed │
   │ (terminal) │  │  Extracted    │  │(retry) │
   └────────────┘  └───────┬───────┘  └────────┘
                           │
                           ▼
                    ┌────────────┐
                    │ Reviewing  │ (patient must review)
                    └──────┬─────┘
                           │ Patient confirms fields
                           ▼
                    ┌────────────┐
                    │ Extracted  │ (terminal after review)
                    └────────────┘
```

**State transition triggers:**

| From | To | Trigger |
|---|---|---|
| Pending | Extracted | Backend returns high-confidence complete extraction |
| Pending | PartiallyExtracted | Backend returns partial extraction (some fields missing/low confidence) |
| Pending | Failed | OCR service error or timeout |
| PartiallyExtracted | Reviewing | System or patient initiates review |
| Failed | Pending | Patient taps "Retry extraction" |
| Reviewing | Extracted | Patient confirms all fields in SCR-03-OCR-REVIEW |

**Figma annotation:**  
Include a state machine diagram on the SCR-03-OCR-REVIEW page (annotation overlay layer) showing these transitions.

---

## Related Documents

- [`figma-component-spec.md`](figma-component-spec.md) — Component anatomy details
- [`figma-accessibility.md`](figma-accessibility.md) — Accessibility requirements per interaction
- [`../navigation-map.md`](../navigation-map.md) — Full navigation map
- [`figma-page-structure.md`](figma-page-structure.md) — Which page each screen lives on
