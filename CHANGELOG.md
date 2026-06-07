<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# std-naming-hound Changelog

## [Unreleased]

## [0.0.9] - 2026-06-07

### Added
- Added a `Copy SQL Column` action to the search result context menu.
- Added a TERM fallback hint that guides users to Builder when no TERM result exists but WORD composition is available.
- Documented the built-in dataset source, license, permitted use scope, and source link in the README and the settings About section.
- Added `meta.json` and `NOTICE.txt` to built-in dataset ZIP exports.
- Added `docs/TODO.md` to track implementation and verification items.

### Changed
- Reworked search result and Stage context menus to use the IntelliJ Action System popup style, including IDE-native hover, disabled, and theme behavior.
- Updated the ToolWindow UI to use IntelliJ `JB*` components and theme colors.
- Unified the settings, Builder add/delete, Stage delete, and SQL copy controls as IntelliJ `ActionToolbar` icon actions.
- Adjusted the top filter and settings control spacing to better match IntelliJ toolbar density.
- Improved Builder and Stage token presentation and interaction.

### Fixed
- Replaced the deprecated `ToolWindowFactory.isApplicable(Project)` path with an `isApplicableAsync` implementation.
- Moved ToolWindow anchor, icon, and activation settings into `plugin.xml` declarations to reduce deprecated, internal, and experimental API usage.
- Migrated the ToolWindow factory implementation to Java to avoid synthetic bridge references generated from the Kotlin implementation.
- Replaced deprecated `LinkLabel.create(String, Runnable)` usage with `HyperlinkLabel`.
- Cleaned up nullable file handling in the base dataset export save flow.

## [0.0.8] - 2026-02-12

### Changed
- Improved Stage and Builder token interaction UX.
- Extracted token drag-and-drop behavior into `TokenDnDHelper`.

## [0.0.7] - 2026-02-11

### Added
- Added repository agent guidelines in `AGENTS.md`.

### Changed
- Updated the settings screen UI.
- Fixed Stage area sizing and layout behavior.
- Updated user-facing constant messages.
- Simplified string construction with Kotlin string templates.

### Fixed
- Fixed typo issues in code and UI text.

### Dependencies
- Updated Kotlin from `2.3.0` to `2.3.10`.

## [0.0.6] - 2026-02-04

### Added
- Added dictionary export support.

### Changed
- Extracted resource loading logic into a reusable utility.
- Replaced magic numbers with named constants.

### Fixed
- Fixed typo issues.

## [0.0.5] - 2026-01-31

### Changed
- Split the large ToolWindow factory implementation into purpose-specific components.
- Replaced selected label-based interactions with `LinkLabel` for a more discoverable UX.
- Simplified collection checks by using Kotlin `any`.

### Documentation
- Added KDoc to clarify implementation details.

### Dependencies
- Updated Gradle Wrapper from `9.3.0` to `9.3.1`.

## [0.0.4] - 2026-01-28

### Changed
- Updated logo and icon assets.
- Adjusted IntelliJ Platform compatibility to support build `251` and platform version `2025.1.1`.

## [0.0.3] - 2026-01-28

### Added
- Added a README demo video reference.
- Added the plugin icon.

## [0.0.2] - 2026-01-27

### Added
- Added initial bundled dictionary data.
- Added domain models for the naming dictionary workflow.
- Added the first MVP ToolWindow flow with search, Builder mode, and result presentation.
- Added SQL generation support for Builder output.
- Added review and styleguide automation workflows.

### Changed
- Refined the ToolWindow Builder, staging, and output flow.
- Moved default domain handling into a companion object.
- Updated plugin run configuration and project assets.

### Fixed
- Fixed sample tests.
- Fixed typo and Kotlin style issues.
- Replaced magic-number logic with clearer code.
- Updated the implementation to use Kotlin `windowed`.

### Documentation
- Updated the README with usage and project information.

### Dependencies
- Updated Gradle Wrapper from `9.2.1` to `9.3.0`.
- Updated Kotlin from `2.2.21` to `2.3.0`.

## [0.0.1] - 2026-01-24

### Added
- Bootstrapped the IntelliJ Platform plugin project with Gradle, Kotlin, plugin metadata, CI workflows, and starter tests.

### Changed
- Cleaned the generated IntelliJ Platform Plugin Template content.
- Renamed the project and package identifiers for `std-naming-hound`.
