# Frontend Skill Research

## Skills Needed

- Advanced CSS layout: needed for premium home, detail, admin, and responsive table surfaces.
- CSS Grid and Flexbox: used for hero, cards, result rows, admin tiles, forms, and action clusters.
- CSS variables/design tokens: required to make the visual system consistent and maintainable.
- Modern color system: needed to move away from flat beige/teal into a layered hospitality palette.
- Soft glass and depth: useful for premium search/booking panels without losing readability.
- 3D CSS transform: useful as subtle ambient decoration and card hover depth.
- Micro-interaction design: buttons, cards, tables, focus states, badges, and nav need better feedback.
- Animation timing/easing: needed for page entrance, hover, and ambient motion.
- Responsive design: required for 375px through large desktop.
- Accessibility: focus rings, contrast, labels, reduced motion, and keyboard-safe controls.
- Dashboard UI design: admin pages need clearer dense tables and calmer operational styling.
- Form UX: search, auth, checkout, booking, and admin forms need stronger focus/error states.
- Performance optimization: animations must use transform/opacity and avoid heavy JS.

## Libraries Considered

- Framer Motion: not used. Project is not React and adding it would be invalid.
- GSAP: not used. Current needs are achievable with CSS transitions/keyframes.
- Three.js: not used. True 3D would add weight and complexity without improving booking flows enough.
- Lenis/AOS/Lottie: not used. No JS build pipeline exists; CSS can cover the needed motion.
- Lucide/Heroicons/Font Awesome: not added. Inline symbolic UI and CSS indicators are enough; adding a dependency is unnecessary.
- Tailwind/Bootstrap: not used. The project already uses custom CSS, and a full utility framework would churn templates heavily.
- CSS-only design system: used. It fits Thymeleaf, has zero bundle cost, and keeps backend routes/forms stable.

## Technical Decisions

| Technique | Use | Rationale | Performance | Fallback |
|---|---:|---|---|---|
| CSS variables | Yes | Centralizes premium tokens and states | Excellent | Static CSS values still cascade |
| CSS Grid/Flex | Yes | Best fit for responsive server-rendered layouts | Excellent | Mobile collapses to single column |
| CSS-only ambient depth | Yes | Adds visual quality without JS | Good if subtle | Disabled by reduced motion |
| Glass surfaces | Yes | Premium hospitality feel around key panels | Good | Solid color fallback if blur unsupported |
| Page/card animation | Yes | Improves perceived polish | Transform/opacity only | `prefers-reduced-motion` disables |
| JS animation libraries | No | No frontend build, unnecessary dependency | Avoids bundle cost | CSS covers interactions |
| True 3D/Three.js | No | Decorative-only need, not worth weight | Avoids runtime cost | CSS perspective/glow |
| New icon package | No | Not necessary for current scope | Avoids dependency | Text + CSS affordances |

## Implementation Notes

- Keep one CSS import path (`/css/app.css`) so existing templates remain simple.
- Internally structure `app.css` by sections: tokens, reset/base, layout, components, pages, animation, responsive.
- Prefer semantic classes already present and add additive classes only where useful.
- Do not add new form handlers or JavaScript-dependent controls.
