# Easy fees: UX design guide

A build spec for recreating the fee-tracking screen of a childcare management app: calm, warm, and built for a nursery manager checking who has paid this week.

## What the screen is for

The person using it is a creche manager or admin. Their job on this screen is to answer one question fast: how much is owed, how much is in, and which families still owe. Everything on screen serves that.

- **Persistent app shell:** left sidebar for navigation, a wide content area for work.
- **Summary first:** two totals sit above the data so the answer is visible before scrolling.
- **Week as the unit of time:** fees are browsed one week at a time with a simple previous/next switcher.
- **Table as the main object:** one row per child, showing the parents billed. Further columns (amount, status, action) continue to the right.

The screenshot is cropped on the right. Columns beyond "Parents" are unseen, so the table spec below marks them as suggestions.

## Layout

Two regions on a soft off-white canvas. The whole app sits in a rounded window with a faint border and a large soft shadow, floating above a decorative mint arc and a small leaf burst (marketing-screenshot styling; skip these in the real app).

┌──────────────┬───────────────────────────────────────┐ │ ◉ Brand │ Easy fees (page title) │ │ │ │ │ ▦ Dashboard │ ┌────────────┐ ┌────────────┐ │ │ ▬ Easy fees◀ │ │ €34,599 │ │ €24,100 │ summary │ │ ⚇ Attendance │ │ TOTAL DUE │ │ PAID │ cards │ │ ▤ Occupancy │ └────────────┘ └────────────┘ │ │ ▯ Reports │ │ │ ▣ Directory │ \[‹\]\[›\] Jul 2023, Week 28 week switcher │ │ ▨ Gallery │ │ │ │ CHILD ↓ PARENTS …more cols │ │ (spacer) │ ────────────────────────────────────── │ │ │ Maria Jones Alison Jones, John Jones │ │ ⚙ Settings │ Aisling … Mary Bryne, Paul Bryne │ │ ◎ Help desk │ … │ │ ▭ Inbox ① │ │ │ ─────────────│ │ │ ◔ Maria ⌄ │ │ └──────────────┴───────────────────────────────────────┘

### Measurements (approximate, at 1x)

| Part | Value | Notes |
| --- | --- | --- |
| Sidebar width | 240px | Slightly darker than the content panel, separated by a 1px line. |
| Content padding | 44px left, 40px top | Content aligns to one left edge: title, cards, switcher, table. |
| Nav item | 40px tall, 10px radius | 16px icon, 12px gap, 12px side padding. |
| Sidebar groups | Top: brand + 6 pages. Bottom: Settings, Help desk, Inbox, then a divider and user menu. | A flexible spacer pushes the bottom group down. |
| Summary cards | \~190px wide, 14px radius, 14px gap | 1px border, no shadow. |
| Table row | 56px tall | 1px bottom rule, no zebra striping, no vertical rules. |
| Table header row | 40px tall | Muted, small, letter-spaced. |

## Color

Warm neutrals do most of the work. Color is reserved for meaning: mint for brand atmosphere, yellow for "owed", green for "paid".

Canvas`#FAFAF8` sidebar, cards

Panel`#FFFFFF` content area

Ink`#232321` titles, values

Muted`#6E6E69` nav, labels, headers

Line`#ECE9E2` borders, row rules

Active`#EDE8DA` selected nav item

Mint`#C3E4D3` decorative arcs, leaves

Due yellow`#F4D24B` total due bar, inbox badge

Paid green`#7ED48E` paid bar

Brand peach`#F0916B` logo face

Status colors are never the only signal. Pair them with the label ("Total due", "Paid") so the screen still works for color-blind users.

## Typography

One friendly, neutral grotesque sans (Inter, Figtree, or Instrument Sans all work). Big text gets tight tracking; small labels get slightly loose tracking. Sentence case throughout, apart from the small column headers and card labels.

| Role | Size / weight | Color |
| --- | --- | --- |
| Page title ("Easy fees") | 28px / 600, tracking −0.03em | Ink |
| Summary value (€34,599) | 24px / 500, tracking −0.03em | Ink |
| Card label, column header | 13px / 400, tracking +0.04em, uppercase | Muted |
| Nav item | 15px / 400 | Muted (Ink + 500 when active) |
| Table cell | 15px / 400 | Ink |
| Week label | 15px / 400 | Ink |

## Components

Live specimens built with plain CSS. Copy the proportions, not the pixels.

### Sidebar navigation

Dashboard

Easy fees

Attendance

Occupancy planner

- Outline icons, 1.5px stroke, matching the text color. Use one icon set throughout (Lucide or Phosphor fit well).
- Active item: filled warm beige pill, darker text, slightly heavier weight. No accent bar.
- Hover: a lighter tint of the active fill. Focus: visible 2px ring.
- Inbox shows a small yellow circular badge with a count, right-aligned.

### Summary cards

**€34,599**TOTAL DUE

**€24,100**PAID

- A short vertical color bar inside the left edge carries the status. Yellow means outstanding, green means received.
- Add a third card for "Overdue" (a soft red bar) when you extend the design.
- Values use tabular numerals so totals don't jitter when the week changes.

### Week switcher

**‹****›**Jul 2023, Week 28

- Two joined icon buttons in one bordered, rounded group, then the label as plain text.
- Label format: month, year, week number. Changing the week reloads the cards and table together.
- Suggested: keyboard arrow keys move between weeks, and a "Today" shortcut appears when you're not on the current week.

### Fees table

CHILD ↓

PARENTS

Maria Jones

Alison Jones, John Jones

Aisling Byrne

Mary Bryne, Paul Bryne

- Airy and quiet: horizontal rules only, generous row height, no cell borders.
- The Child header has a sort arrow, so sorting by name is the default and the column is clickable.
- Parents are comma-separated names in one cell. Truncate with an ellipsis and reveal on hover if space runs out.
- Suggested hidden columns: amount due, amount paid, status chip (Paid, Part-paid, Due), and a row action.

The sample data repeats "Maria Jones" and "Leo Wilson" on separate rows, likely because one child can have several invoices. In your build, add a second line or a column (fee type, session) so identical names can be told apart.

## Surfaces, radius and depth

- **Radius scale:** 10px for controls and nav items, 14px for cards, 28px for the outer app window.
- **Borders over shadows:** inner elements use 1px warm-grey borders. Only the outer window gets a large, very soft shadow.
- **Spacing rhythm:** 4px base unit. Common steps: 8, 12, 16, 24, 40.
- **Density:** comfortable, not compact. Whitespace does the grouping, so avoid extra dividers.

## States and behavior to design

The screenshot shows only the happy path. These are the moments to add.

- **Loading:** skeleton rows at 56px height and shimmering card values, so the layout doesn't jump.
- **Empty week:** "No fees for this week. Fees appear once children are booked in." with a link to the Occupancy planner.
- **Row hover and click:** subtle tinted row; clicking opens a side panel with invoice details and a "Record payment" button.
- **Payment recorded:** the card values update in place and a toast says "Payment recorded".
- **Errors:** say what failed and how to retry, for example "Couldn't load fees. Check your connection and try again."
- **Responsive:** below \~900px, collapse the sidebar to an icon rail or a drawer, and let the table scroll horizontally inside its own container.
- **Accessibility:** 4.5:1 contrast on all text (check the muted grey on the beige active fill), visible focus rings, and real button and table elements.

## Copy and voice

- Plain, friendly, sentence case: "Easy fees", "Occupancy planner", "Help desk".
- Money as €34,599 with a thousands separator and no decimals in summaries. Show decimals in row-level amounts.
- Buttons say what they do: "Record payment", "Send reminder", "Export fees". The toast reuses the same verb.

## Build notes

- **Stack:** React with Tailwind and shadcn/ui components (Table, Button, Badge). Use CSS variables for the tokens above so dark mode is a variable swap.
- **Structure:** `AppShell` (sidebar + main), `SummaryCard`, `WeekSwitcher`, `FeesTable`.
- **Data:** `fees(weekStart)` returns rows of `{ childId, childName, parents[], amountDue, amountPaid, status }`. Summary totals are sums over the same response, so cards and table never disagree.
- **Order of work:** tokens, then shell and nav, then cards, then table with static data, then week switching, then states.