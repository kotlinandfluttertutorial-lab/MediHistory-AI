# 01 — Dashboard Screen Prompt for Google Stitch

> **Pre-requisite:** Paste `00-master-stitch-prompt.md` first in this Google Stitch session, then paste this prompt.

---

## Screen Prompt

Design the **Patient Dashboard** screen for MediHistory AI — a native Android application.

**Screen ID:** SCR-01-DASHBOARD
**Feature:** F01 — Patient Dashboard
**Linked requirements:** FR-001, FR-002, FR-003, FR-004
**Acceptance criteria:** AC-001, AC-002, AC-003, AC-004, AC-005

---

## Screen Purpose

The dashboard is the home screen. It gives the patient a glanceable overview of their entire medical record — total prescriptions, active doctors, medicine count, and pending alerts — and provides quick-access shortcuts to the most common tasks. It must orient first-time users without being overwhelming, and give returning users a meaningful summary without clicking through to sub-screens.

---

## Device and Layout Context

- Phone portrait, reference width: **360dp**
- The screen uses a `LargeTopAppBar` that collapses into a `SmallTopAppBar` as the patient scrolls.
- A persistent `BottomNavigationBar` appears at the bottom of every screen.
- The Dashboard tab is selected (filled home icon with indicator pill).

---

## UI Element Specification

### 1. Large Top App Bar (collapsing)

- **Large (expanded) state:**
  - Title: "Good morning, Priya 👋" — `displaySmall`, `on-surface` colour.
  - Subtitle: "Tue, 15 Jul 2025" — `bodyMedium`, `outline` colour.
  - Trailing actions: Notification bell icon button (badge showing "2" for 2 pending alerts) + Profile avatar (Priya's initials "PS" in a filled circle).
- **Collapsed state (on scroll):**
  - Single-line title: "MediHistory AI"
  - Same trailing icons.

### 2. Summary Stats Row — 4 OutlinedCards

Place four equal-width `OutlinedCard` tiles in a 2×2 grid. Each tile:
- Icon at top-left (tinted with `primary` colour).
- Large number (`headlineMedium`, bold) on the next line.
- Label (`bodySmall`, `outline` colour) below the number.
- The "Alerts" tile: number is `error` colour if > 0.

| Tile | Icon | Number | Label |
|---|---|---|---|
| Prescriptions | receipt_long | 6 | Total prescriptions |
| Doctors | medical_services | 4 | Doctors |
| Medicines | medication | 12 | Active medicines |
| Alerts | warning_amber | 2 | Pending alerts |

Cards are arranged: [Prescriptions] [Doctors] on row 1, [Medicines] [Alerts] on row 2. Horizontal spacing 8dp between tiles. All four tiles have equal height.

The "Alerts" card has a subtle pulsing amber ring animation (gentle, not distracting) to draw attention when alerts > 0.

### 3. Pending Actions Card

A single `ElevatedCard` below the stats grid labelled "Needs your attention" (`titleMedium`, leading icon: notifications_active).

Shows a vertical list of up to 3 action items with leading icons:

```
⚠  OCR review needed — Dr. Mehta's prescription (3 Jul)     [Review →]
🔔 2 potential duplicate ingredient alerts                    [View →]
✓  Medical history last updated 12 days ago                  [Update →]
```

Each row: leading icon, text (`bodyMedium`), trailing `TextButton` "action →".
Card is tappable — tap on an action navigates to the relevant screen.
If no pending actions: card reads "All caught up ✓" in `bodyMedium` with a green check icon — card still visible, not hidden.

### 4. Recent Activity List

Section header: "Recent activity" — `titleMedium`, with a trailing "See all" `TextButton`.

Show 3 recent activity items as `ElevatedCard` tiles (narrower, list-style):

```
Card 1:
  [📋 prescription thumbnail 56×56dp] | Dr. Rajan Mehta – Orthopedics
                                      | Added 3 days ago · 3 medicines
                                      | [Chip: Reviewed ✓]

Card 2:
  [📋 prescription thumbnail 56×56dp] | Dr. Ananya Krishnamurthy – Cardiology
                                      | Added 18 days ago · 5 medicines
                                      | [Chip: OCR in progress…]

Card 3:
  [📋 prescription thumbnail 56×56dp] | Dr. Sunita Bose – General
                                      | Added 25 days ago · 4 medicines
                                      | [Chip: Needs review ⚠]
```

Each card: `ElevatedCard`, thumbnail on the left (rounded corners 8dp), doctor name as `titleMedium`, secondary line as `bodySmall`, status chip (`AssistChip`) bottom-left of the text block.

"Needs review" chip: amber/tertiary colour scheme with ⚠ icon.
"Reviewed" chip: secondary-container colour with ✓ icon.
"OCR in progress" chip: primary-container colour with animated circular progress icon.

### 5. Quick Action Buttons Row

A horizontally scrollable row of `ElevatedButton` or large icon+label buttons just above the bottom navigation, placed in a horizontal `Row`:

| Button | Icon | Label |
|---|---|---|
| Add Prescription | add_photo_alternate | Add Prescription |
| Check Duplicates | compare_arrows | Check Duplicates |
| Medical History | history_edu | My History |
| New Doctor Mode | person_add | New Doctor |

Each button: icon top-centred, label below, square card shape (72×72dp), `FilledTonalButton` style. Row scrolls horizontally to avoid wrapping.

### 6. FAB

- Position: lower-right, above bottom nav.
- Icon: `add` (plus).
- On scroll down: FAB collapses to icon-only.
- On scroll up: FAB extends to show label "Add Prescription".
- Tapping FAB navigates to the "Add Prescription" flow.

### 7. Bottom Navigation Bar

5 items: Dashboard (selected), Prescriptions, Medicines, History, More.
Prescriptions tab has a badge "1" (one prescription pending OCR review).
History tab has no badge.

---

## Screen States

### State 1: Loading (skeleton)

Display skeleton shimmer for:
- Top app bar greeting text.
- All 4 stat tiles (show shimmer rectangles in place of numbers and labels).
- Pending actions card (2 shimmer rows).
- 3 recent activity cards (shimmer thumbnails + shimmer text lines).

No spinner, no blank screen. Skeleton layout must match the populated layout geometry.

### State 2: First Use / Empty

- Stats row: all tiles show "0" with `outline` colour numbers.
- Pending actions card: replaced by an `ElevatedCard` with:
  - Illustration: doctor holding a clipboard (friendly, line-art style).
  - Title: "Welcome, Priya!" (`headlineMedium`)
  - Body: "Add your first prescription to get started. MediHistory AI will organise your medicines and help you prepare for your next doctor visit." (`bodyMedium`)
  - `ExtendedFloatingActionButton` style CTA: `+  Add first prescription`
- Recent activity section: hidden (not shown on first use, or shows empty state illustration: "No activity yet").

### State 3: Populated (Priya's data)

All elements shown as specified above using Priya Sharma's synthetic data:
- 6 prescriptions, 4 doctors, 12 active medicines, 2 duplicate alerts.
- Recent activity: Dr. Mehta (3 days ago, Reviewed), Dr. Krishnamurthy (18 days ago, OCR in progress), Dr. Bose (25 days ago, Needs review).
- Pending actions: OCR review needed, 2 duplicate alerts, history 12 days old.

---

## Interactions and Navigation

| Trigger | Destination |
|---|---|
| Tap "Prescriptions" stat tile | SCR-02-RX-LIST |
| Tap "Medicines" stat tile | SCR-04-MED-EXPLORER |
| Tap "Alerts" stat tile | SCR-05-DUPLICATE-REVIEW |
| Tap "Review →" on OCR action | SCR-02-RX-DETAIL (needs review prescription) |
| Tap "View →" on duplicate action | SCR-05-DUPLICATE-REVIEW |
| Tap "Update →" on history action | SCR-06-HISTORY |
| Tap any recent activity card | SCR-02-RX-DETAIL |
| Tap "Add Prescription" quick action | Add Prescription bottom sheet |
| Tap "Check Duplicates" quick action | SCR-05-DUPLICATE-REVIEW |
| Tap "My History" quick action | SCR-06-HISTORY |
| Tap "New Doctor" quick action | SCR-07-NEW-DOCTOR |
| Tap FAB | Add Prescription bottom sheet |
| Tap notification bell | Notifications panel (bottom sheet) |

---

## Accessibility Specifications

- Greeting text `contentDescription`: "Good morning, Priya Sharma. Today is Tuesday, 15 July 2025."
- Each stat tile `contentDescription`: e.g., "12 active medicines. Tap to view medicine list."
- Alerts stat tile: "2 pending duplicate alerts. Tap to review."
- FAB `contentDescription`: "Add prescription"
- Notification bell: "Notifications. 2 unread alerts."
- Recent activity cards: "Prescription from Dr. Rajan Mehta, Orthopedics, added 3 days ago. Status: Reviewed."
- All shimmer elements: `contentDescription="Loading"` on the container.
- "Needs review" chips: announced as "Needs review — this prescription has fields requiring your attention."

---

## Additional Design Notes

- The overall tone is warm and reassuring — medical without being clinical or cold.
- Use subtle rounded containers and gentle colour fills; avoid sharp contrasts that feel alarming.
- The greeting card area should feel personalized — like a health companion, not a hospital system.
- Padding between sections: 24dp. Padding between cards within a section: 8dp.
- The stats grid tiles use `OutlinedCard` (not filled) so the screen doesn't feel heavy.
- The pending actions card uses a left `primary`-coloured accent border (4dp) to draw attention without using alarm colours.

---

## Feature and Requirement Traceability

| Element | Requirement | Acceptance Criteria |
|---|---|---|
| Summary stats row | FR-001 | AC-001: Stats visible on dashboard load |
| Stats reflect real counts | FR-002 | AC-002: Counts match actual records |
| Pending alerts visible | FR-003 | AC-003: Duplicate alert count shown, tappable |
| Recent activity shown | FR-004 | AC-004: Last 3 activities listed |
| Quick navigation to modules | FR-001 | AC-005: All main modules reachable from dashboard |
| Empty state onboarding | FR-001 | AC-001: Empty state shows CTA for first use |
| Skeleton loading state | FR-001 | AC-001: No blank screen during load |
