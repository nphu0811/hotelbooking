# Frontend Design System

## 5.1 Color Tokens

| Token | Value | Use |
|---|---|---|
| Primary | `#2563EB` | Main CTAs, active states |
| Primary dark | `#1E3A8A` | Hover and strong text |
| Secondary | `#4F46E5` | Admin/product accents |
| Accent | `#06B6D4` | Price, focus, and premium highlights |
| Background | `#EEF4FF` | App canvas |
| Background deep | `#0F172A` | Hero/nav contrast |
| Surface | `#F8FBFF` | Cards and forms |
| Surface elevated | `rgba(248,251,255,0.9)` | Glass panels |
| Text primary | `#101828` | Main content |
| Text secondary | `#334155` | Body secondary |
| Muted text | `#64748B` | Metadata |
| Border | `rgba(15,23,42,0.12)` | Containers |
| Success | `#059669` | Success states |
| Warning | `#7C3AED` | Holds/pending with cool violet treatment |
| Error | `#BE123C` | Errors/destructive |
| Info | `#0284C7` | Informational states |
| Gradient primary | `linear-gradient(135deg,#2563EB,#4F46E5 48%,#06B6D4)` | Primary CTA |
| Gradient ambient | `radial-gradient(circle at 18% 20%,rgba(79,70,229,.18),transparent 34%), radial-gradient(circle at 78% 12%,rgba(6,182,212,.18),transparent 30%)` | Page depth |
| Glow | `rgba(6,182,212,0.22)` | Hover/focus glow |

## 5.2 Typography

- Font: `Inter`, `ui-sans-serif`, `system-ui`, `-apple-system`, `BlinkMacSystemFont`, `Segoe UI`, sans-serif.
- Heading scale: 56/44/34/28/22px with tight line height.
- Body: 16px, line height 1.65.
- Small text: 13-14px.
- Label: 13px, uppercase tracking for operational labels.
- Button text: 14px, weight 800.
- Letter spacing: normal for body, subtle positive tracking for labels/eyebrows.

## 5.3 Layout System

- Container max width: 1180px.
- Wide visual sections: 1280px max.
- Header height: 72px target.
- Section spacing: 56-88px desktop, 28-48px mobile.
- Card grid: `repeat(auto-fit, minmax(260px, 1fr))`.
- Search rows: image/content/action grid on desktop; single column on mobile.
- Admin dashboard: 3-up stat grid; tables scroll horizontally on small screens.
- Breakpoints: 1024px, 860px, 640px, 430px.

## 5.4 Component System

- Buttons: pill radius, gradient primary, hover lift, press compression, strong focus ring.
- Inputs/selects/textarea: elevated cool white background, clear focus ring, minimum 44px height.
- Cards: 12-18px radius, thin border, layered shadow, image zoom on hover.
- Tables: sticky-feeling header style, row hover, compact action groups, horizontal scroll on mobile.
- Badges: pill with semantic colors based on status text where possible.
- Alerts: left-accented glass panels with icon-like dot via CSS.
- Navbar: sticky glass topbar, animated link hover, role-aware visibility preserved.
- Empty state: centered elevated panel with ambient decoration.
- Booking/search panels: glass card with stronger CTA and microcopy.
- Admin tiles: metric cards with subtle gradient border.

## 5.5 Motion System

- Fast: 140ms for press/focus.
- Normal: 220ms for hover and state changes.
- Slow: 520ms for page/hero entrance.
- Easing: `cubic-bezier(.2,.8,.2,1)` and spring-like `cubic-bezier(.16,1,.3,1)`.
- Page enter: fade + 10px translate.
- Card hover: translateY(-4px), image scale 1.035.
- Button hover: translateY(-1px), gradient shift, shine overlay.
- Form focus: border/glow transition.
- Table row hover: background and translate 1px.
- Ambient decoration: slow transform/opacity only.
- Reduced motion: all animation and transitions collapse through `prefers-reduced-motion`.
