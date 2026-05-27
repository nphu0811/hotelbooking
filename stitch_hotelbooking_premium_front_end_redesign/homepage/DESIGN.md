---
name: Modern Platinum Luxury
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#444748'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#747878'
  outline-variant: '#c4c7c7'
  surface-tint: '#5f5e5e'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#1c1b1b'
  on-primary-container: '#858383'
  inverse-primary: '#c8c6c5'
  secondary: '#5d5e5f'
  on-secondary: '#ffffff'
  secondary-container: '#e0dfdf'
  on-secondary-container: '#626363'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#1a1c1c'
  on-tertiary-container: '#838484'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e5e2e1'
  primary-fixed-dim: '#c8c6c5'
  on-primary-fixed: '#1c1b1b'
  on-primary-fixed-variant: '#474746'
  secondary-fixed: '#e3e2e2'
  secondary-fixed-dim: '#c6c6c6'
  on-secondary-fixed: '#1a1c1c'
  on-secondary-fixed-variant: '#464747'
  tertiary-fixed: '#e2e2e2'
  tertiary-fixed-dim: '#c6c6c6'
  on-tertiary-fixed: '#1a1c1c'
  on-tertiary-fixed-variant: '#454747'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  headline-xl:
    fontFamily: Montserrat
    fontSize: 48px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Montserrat
    fontSize: 32px
    fontWeight: '600'
    lineHeight: '1.3'
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Montserrat
    fontSize: 28px
    fontWeight: '600'
    lineHeight: '1.3'
  headline-md:
    fontFamily: Montserrat
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.4'
  body-lg:
    fontFamily: Montserrat
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Montserrat
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
  label-sm:
    fontFamily: Montserrat
    fontSize: 12px
    fontWeight: '600'
    lineHeight: '1'
    letterSpacing: 0.05em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  unit: 8px
  gutter: 24px
  margin-desktop: 80px
  margin-mobile: 20px
  container-max: 1280px
---

## Brand & Style
The design system embodies a "Soft Minimalist" approach to high-end luxury, specifically tailored for a premium hospitality experience. It targets a discerning global audience, with specialized attention to the Vietnamese market through impeccable typographic support. 

The aesthetic is clean and ethereal, favoring expansive whitespace and a "cool" metallic palette. By blending the precision of modern minimalism with soft, organic shapes, the UI evokes an emotional response of calm, exclusivity, and effortless sophistication. The visual narrative moves away from traditional "gold and marble" luxury toward a more contemporary "silver and light" tech-forward elegance.

## Colors
This design system utilizes a palette of cool metallics and deep charcoal to establish a "Platinum" edge. 

- **Primary:** A deep Onyx (#1A1A1A) used for high-contrast text and grounding elements.
- **Silver & Platinum Highlights:** A trio of metallic tones (#E5E4E2, #C0C0C0, #D1D1D1) are used for interactive states, subtle borders, and background layering.
- **Base:** A crisp, cool neutral white (#F8F9FA) provides the canvas, ensuring the silver accents feel luminous rather than heavy.

Gradients should be used sparingly, restricted to a very subtle linear flow between Silver and Platinum to simulate a brushed metal texture on primary calls-to-action.

## Typography
Montserrat is the foundational typeface, chosen for its geometric purity and excellent support for Vietnamese diacritics. 

The typographic hierarchy relies on significant scale differences and generous line heights to maintain a "breathable" feel. Headlines use tighter letter spacing and heavier weights to command attention, while body text is spaced for maximum legibility. For labels and micro-copy, a slight tracking increase and uppercase transformation are applied to evoke the feel of premium watchmaking or fashion branding.

## Layout & Spacing
The layout follows a **Fixed Grid** model on desktop to create a sense of composed, editorial balance, transitioning to a fluid model on mobile devices.

- **Desktop:** A 12-column grid with wide 80px outer margins. Content is often centered with significant "negative space" to emphasize the luxury of the imagery.
- **Rhythm:** An 8px linear scale governs all padding and margins. 
- **Adaptation:** On mobile, margins compress to 20px, and grid-heavy layouts reflow into a single-column stack, prioritizing high-resolution vertical imagery.

## Elevation & Depth
Depth is communicated through **Tonal Layers** and **Ambient Shadows** rather than harsh borders. 

Surfaces use the Platinum and Silver tones to create hierarchy. A "floating" effect is achieved using extremely soft, diffused shadows (0% spread, 20px+ blur, 4% opacity) that make cards and buttons appear to hover slightly above the mist-colored background. Glassmorphism is used selectively on navigation overlays, utilizing a high-intensity backdrop blur (20px) and a thin, 1px semi-transparent silver stroke to define edges.

## Shapes
The shape language is defined by a "Pill-Shaped" philosophy. To achieve the requested soft and smooth feel, all interactive elements—including buttons, input fields, and tags—utilize a full corner radius. 

Large containers and cards utilize a minimum of 24px (1.5rem) corner radius. This hyper-roundedness contrasts with the sharp, geometric lines of the Montserrat typeface, creating a balance between architectural precision and welcoming softness.

## Components
- **Buttons:** Primary buttons are fully pill-shaped. They feature either a solid Onyx background with white text or a subtle Platinum-to-Silver gradient. The hover state should involve a gentle "lift" via shadow expansion.
- **Cards:** Cards are pure white or very light Platinum (#E5E4E2) with 24px rounded corners. They should not have heavy borders; instead, use a 1px silver stroke or a soft ambient shadow to separate them from the background.
- **Input Fields:** Search bars and form inputs are pill-shaped with a subtle Silver border. On focus, the border transitions to a slightly darker Silver with a soft outer glow.
- **Chips/Labels:** Used for room categories or amenities, these are small pill-shaped elements with light Platinum backgrounds and uppercase Onyx text.
- **Lists:** Hospitality menus or service lists should feature generous vertical padding (24px+) between items, separated by a thin, 1px Platinum divider that doesn't reach the full width of the container.