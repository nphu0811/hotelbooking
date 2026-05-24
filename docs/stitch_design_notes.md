# Stitch Design Notes

- Stitch project created: `projects/2958367575933304799`.
- Attempted to create a Stitch design-system asset through `create_design_system`.
- Result: Stitch MCP returned `Request contains an invalid argument` for both full and concise payloads.
- Error log: `ERROR-007`.
- Fallback: implemented the design system directly in `docs/design_system.md`, `src/main/resources/static/css/app.css`, and Thymeleaf templates.
- 2026-05-23 retry: uploaded `docs/design_system.md` to Stitch project as screen instance `8194892506987800513`, then `create_design_system_from_design_md` still returned `Request contains an invalid argument`.
