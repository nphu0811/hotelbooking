# Grapuco Notes

- Grapuco semantic search was run for existing HotelBooking code context.
- Grapuco staleness check reported the graph is stale and recommended a full update.
- Grapuco staleness was checked again after implementation and still recommended a full update.
- Current exposed Grapuco tools do not include a repository sync/update command; only search/context/staleness/ERD update tools are visible.
- Implementation proceeded with local source inspection and documented this limitation.
- 2026-05-23: Grapuco bootstrap found repository `hotelbooking` (`100f687e-6707-4930-985c-cee3d13b820a`), but semantic search results referenced older `com.example.hotelbooking` paths instead of the current `com.example.demo` source tree. Current work therefore used Grapuco only as a coarse signal and relied on local source reads/tests for exact evidence.
