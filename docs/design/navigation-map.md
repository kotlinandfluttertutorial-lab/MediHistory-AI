# Navigation Map — MediHistory AI

**Version:** 1.0  
**Last updated:** 2025  
**Figma reference:** https://www.figma.com/design/xLPxdsIl7CBJOJTtliHKj5/MediHis — Page 17: `17 — Flows & Nav Map`

---

## 1. Screen Hierarchy

```
App Root
├── Onboarding / Authentication (AUTH)
│   ├── Welcome screen
│   ├── Sign-in screen
│   └── Biometric / PIN setup
│
└── Main App (after authentication)
    ├── [Tab 1] Dashboard (SCR-01-DASHBOARD)
    │
    ├── [Tab 2] Prescriptions
    │   ├── Prescription List (SCR-02-RX-LIST)
    │   │   ├── Doctor Group View (SCR-02-DOCTOR-GROUP)  [toggle/mode]
    │   │   └── Prescription Detail (SCR-02-RX-DETAIL)
    │   │       └── OCR Review (SCR-03-OCR-REVIEW)       [from status chip]
    │   └── Add Prescription Flow
    │       ├── Capture / Pick image (CameraX / Photo Picker)
    │       └── → OCR Review (SCR-03-OCR-REVIEW)
    │
    ├── [Tab 3] Medicines
    │   ├── Medicine Explorer (SCR-04-MED-EXPLORER)
    │   └── Medicine Detail (SCR-04-MED-DETAIL)
    │       └── → Prescription Detail (SCR-02-RX-DETAIL) [source link]
    │
    ├── [Tab 4] History
    │   ├── Medical History (SCR-06-HISTORY)
    │   │   └── → Prescription Detail (SCR-02-RX-DETAIL) [source citation]
    │   └── New Doctor Mode (SCR-07-NEW-DOCTOR)
    │       └── PDF Preview (SCR-07-PDF-PREVIEW)
    │
    └── [Tab 5] Safety
        ├── Safety Center (SCR-09-SAFETY-CENTER)
        │   └── → Medicine Detail (SCR-04-MED-DETAIL)   [from result card]
        ├── Duplicate Review (SCR-05-DUPLICATE-REVIEW)
        │   └── → Medicine Detail (SCR-04-MED-DETAIL)   [from alert card]
        └── Reminders (SCR-08-REMINDERS)               [accessible from Safety or Settings]
```

---

## 2. Bottom Navigation Destinations

The bottom navigation bar is visible on all top-level destinations. It is hidden on detail screens and modal flows to maximize screen space.

| Tab index | Destination | Screen ID | Icon | Badge |
|---|---|---|---|---|
| 1 | Dashboard | SCR-01-DASHBOARD | `dashboard` | None |
| 2 | Prescriptions | SCR-02-RX-LIST | `description` | None |
| 3 | Medicines | SCR-04-MED-EXPLORER | `medication` | None |
| 4 | History | SCR-06-HISTORY | `history_edu` | None |
| 5 | Safety | SCR-09-SAFETY-CENTER | `health_and_safety` | Duplicate alert count (when > 0) |

**Visibility rules:**
- Bottom nav is visible: SCR-01, SCR-02-RX-LIST, SCR-04-MED-EXPLORER, SCR-06-HISTORY, SCR-09-SAFETY-CENTER
- Bottom nav is hidden: all detail screens, OCR review, New Doctor Mode, PDF Preview, Reminders, Settings, Consent dialogs

---

## 3. Modal and Bottom Sheet Flows

These flows appear as overlays on top of the current screen. They do not replace the back stack destination.

| Modal | Trigger | Dismiss | Back stack effect |
|---|---|---|---|
| ConsentDialog (Share) | "Share" button on SCR-07-PDF-PREVIEW | Cancel / system share completes | None (returns to SCR-07-PDF-PREVIEW) |
| ConsentDialog (Export) | Export action in SCR-06-HISTORY | Cancel / export completes | None |
| ConsentDialog (Delete) | Delete action in settings or context menu | Cancel / deletion completes | Returns to previous screen (or auth if all data deleted) |
| ConsentDialog (Cloud Backup) | Cloud backup toggle in Settings | Cancel / confirmed | None |
| Add Prescription Bottom Sheet | FAB on SCR-02-RX-LIST | Cancel | None |
| Image Source Picker Bottom Sheet | From Add Prescription sheet | Cancel / selection made | None |
| ConfidenceBadge Tooltip | Long-press on badge | Tap away / 3 sec timeout | None |
| Overflow Menu (TopAppBar) | Overflow icon tap | Tap away / selection made | None |
| Snackbar | Various actions | Auto-dismiss / Undo tap | None |

---

## 4. Deep Link Patterns

Deep links are handled by the main `Activity` with `NavController`. Each deep link reconstructs a synthetic back stack.

### 4.1 Deep Link Definitions

| Scheme | Path | Parameters | Destination | Synthetic back stack |
|---|---|---|---|---|
| `medihistory://` | `reminder/{reminderId}` | `reminderId: String` | SCR-08-REMINDERS, scrolled to reminder | Dashboard → Safety → Reminders |
| `medihistory://` | `duplicate/{alertId}` | `alertId: String` | SCR-05-DUPLICATE-REVIEW, scrolled to alert | Dashboard → Safety → Duplicate Review |
| `medihistory://` | `prescription/{prescriptionId}` | `prescriptionId: String` | SCR-02-RX-DETAIL | Dashboard → Prescriptions → Rx Detail |
| `medihistory://` | `medicine/{medicineId}` | `medicineId: String` | SCR-04-MED-DETAIL | Dashboard → Medicines → Med Detail |
| `medihistory://` | `import` | None | Add prescription flow (pick source) | Dashboard → Prescriptions → Add |
| `medihistory://` | `history` | None | SCR-06-HISTORY | Dashboard → History |

### 4.2 Deep Link Registration

Deep links must be registered in `AndroidManifest.xml` with `<intent-filter>` for `ACTION_VIEW` and the custom scheme. Only the `Main` activity handles deep links.

### 4.3 Unauthenticated Deep Links

If a deep link arrives and the user is not authenticated, the app navigates to sign-in first, then resumes the deep link destination after successful authentication.

---

## 5. Back Stack Behavior Per Flow

### 5.1 Standard Drill-Down

```
Dashboard → Rx List → Rx Detail → OCR Review

Back from OCR Review     → Rx Detail
Back from Rx Detail      → Rx List
Back from Rx List        → (system back / exit if Rx List is a tab root)
```

### 5.2 Cross-Tab Navigation (Source Links)

Source links in History or Medicine Detail navigate to a prescription detail screen. These navigations are treated as push navigations and do not switch tabs.

```
History → [tap source citation] → Rx Detail (pushed onto History back stack)
Back from Rx Detail → History
```

This preserves the user's context in the History tab.

### 5.3 New Doctor Mode Flow

```
History → [tap "New Doctor Mode"] → New Doctor Mode → PDF Preview → [share] → History
                                                       [back]       → New Doctor Mode
                                  [back]               → History
```

### 5.4 Add Prescription Flow

```
Rx List → [FAB] → Bottom Sheet (pick source)
  → Camera Capture → OCR Review → [confirm] → Rx Detail (newly created) → Rx List
                                  [discard]  → Rx List (no prescription saved)
  → Photo Picker  → OCR Review → same as above
```

### 5.5 Deep Link: Duplicate Alert Notification

```
Notification → [tap] → duplicate/{alertId}
  → Dashboard (synthetic) → Safety (synthetic) → SCR-05-DUPLICATE-REVIEW (scrolled to alert)
  Back → Safety
  Back → Dashboard
```

---

## 6. Entry Points From Notifications

Notifications are the only external entry points into the app (besides cold launch). All notification entry points must deep link into the appropriate screen.

### 6.1 Reminder Notification

| Notification element | Action | Destination |
|---|---|---|
| Tap notification body | Deep link | SCR-08-REMINDERS (scrolled to the reminder) |
| "Log dose" quick action | Log dose directly | No navigation (dose logged in background); snackbar confirmation on return |
| "Snooze" quick action | Snooze reminder | No navigation; reminder rescheduled |

### 6.2 Duplicate Alert Notification

| Notification element | Action | Destination |
|---|---|---|
| Tap notification body | Deep link | SCR-05-DUPLICATE-REVIEW (scrolled to the alert) |
| "Review" quick action | Deep link | Same destination |
| "Dismiss" quick action | Dismiss alert | No navigation; alert marked dismissed |

### 6.3 Sync Status Notification (optional, background only)

- Sync success: no notification
- Sync failure: persistent notification with "Retry" action and "Dismiss" action
- Tapping sync failure notification: opens Settings → Sync status screen

---

## 7. Navigation Technical Notes

### 7.1 Compose Navigation Setup

Use **Jetpack Navigation Compose** (`navigation-compose`). Route definitions:

```
Route examples (illustrative — not final implementation):
"dashboard"
"prescriptions"
"prescriptions/{prescriptionId}"
"prescriptions/{prescriptionId}/ocr-review"
"medicines"
"medicines/{medicineId}"
"history"
"history/new-doctor"
"history/new-doctor/pdf-preview"
"safety"
"safety/duplicates"
"safety/duplicates/{alertId}"
"safety/reminders"
```

### 7.2 Multi-Back-Stack for Tabs

Use `saveState = true` and `restoreState = true` when navigating between tabs so each tab maintains its own back stack. This is the standard `NavController.navigate(route) { launchSingleTop = true; saveState = true }` pattern.

### 7.3 Preventing Duplicate Navigation

Use `launchSingleTop = true` for all tab navigation actions to prevent duplicate entries in the back stack when tapping the already-selected tab.

### 7.4 Authentication Guard

A `NavController` global route listener checks authentication state before allowing navigation to any main app destination. Unauthenticated users are redirected to the auth graph.

---

## Related Documents

- [`figma-interaction-spec.md`](figma/figma-interaction-spec.md) — Transitions, gestures, and deep link behavior
- [`screen-inventory.md`](screen-inventory.md) — Full screen list
- [`screen-to-feature-mapping.csv`](screen-to-feature-mapping.csv) — Screen-to-feature traceability
