# Changelog

All notable changes to Archie are documented here, generated automatically from merged pull requests and direct commits following [Conventional Commits](https://www.conventionalcommits.org/).

## [1.0.0] - 2026-08-13

### Features

- capability-lookup wrapper (Phase 1) + public GameTest assertions ([#12](https://github.com/kernel-panic-codecave/Archie/pull/12)) - @KP2048
- data-attachment wrapper (Phase 2) ([#13](https://github.com/kernel-panic-codecave/Archie/pull/13)) - @KP2048
- item-backed container menus (Phase 3) ([#14](https://github.com/kernel-panic-codecave/Archie/pull/14)) - @KP2048
- Compose-vanilla focus bridge, theme system overhaul, and bedrock theme ([#16](https://github.com/kernel-panic-codecave/Archie/pull/16)) - @KP2048

### Bug Fixes

- address unresolved Copilot review feedback from PRs #3, #5, #6 ([#8](https://github.com/kernel-panic-codecave/Archie/pull/8)) - @KP2048
- NBT serialization crash in ArchieFluidSlot/Storage and ArchieEnergyStorage ([#9](https://github.com/kernel-panic-codecave/Archie/pull/9)) - @KP2048
- give client GameTests a virtual clock/dispatcher instead of real threads ([#10](https://github.com/kernel-panic-codecave/Archie/pull/10)) - @KP2048
- stop mike from writing symlinks into gh-pages, breaking Pages deploy ([#18](https://github.com/kernel-panic-codecave/Archie/pull/18)) - @KP2048
- network stream desync from mismatched write/read byte-array framing ([#19](https://github.com/kernel-panic-codecave/Archie/pull/19)) - @KP2048
- stop submitting the whole ever-growing CHANGELOG.md as the Modrinth version body ([#20](https://github.com/kernel-panic-codecave/Archie/pull/20)) - @KP2048

### Refactoring

- move to kotlin exepct/actual declarations ([6e31ba2](https://github.com/kernel-panic-codecave/Archie/commit/6e31ba251fc5795cfa9002dc672a47644b4fc1b6)) - Witherking25

### Documentation

- overhaul KDoc coverage and markdown guides, refresh Archie-Test ([0d7cb61](https://github.com/kernel-panic-codecave/Archie/commit/0d7cb612c6925ddf47c978916440ff1ef551cdb1)) - KernelPanic
- fix wrong Catalogue-is-broken claim in config.md ([#11](https://github.com/kernel-panic-codecave/Archie/pull/11)) - @KP2048
- fix all unresolved KDoc links flagged by the docs build ([#17](https://github.com/kernel-panic-codecave/Archie/pull/17)) - @KP2048

### Tests

- migrate unit suites and extract archie test sources ([6f1f76d](https://github.com/kernel-panic-codecave/Archie/commit/6f1f76d396be0e168b17fdab0e46bf98f8f2a1c7)) - KernelPanic

### Build System

- moved all the precompiled script plugins to their own seperate project ([e7a43dc](https://github.com/kernel-panic-codecave/Archie/commit/e7a43dc7fa7a2594b6f3606997458609104c9897)) - KernelPanic
- collapse Archie/Archie-Core/Archie-Test into one Loom build at repo root ([#15](https://github.com/kernel-panic-codecave/Archie/pull/15)) - @KP2048

### CI/CD

- fix 401 resolving MrCrayfish GitHub Packages deps ([#4](https://github.com/kernel-panic-codecave/Archie/pull/4)) - @KP2048
- automated changelog + release-notes blog post on tag ([#6](https://github.com/kernel-panic-codecave/Archie/pull/6)) - @KP2048
- fix changelog ordering for modpublisher's own tag/release publishing ([#7](https://github.com/kernel-panic-codecave/Archie/pull/7)) - @KP2048

### Chores

- convert repo to composite builds ([bbe1865](https://github.com/kernel-panic-codecave/Archie/commit/bbe18655a7bcaf1a7e477137df4c825c3a36b397)) - KernelPanic
- finish composite-build conversion and fix fabric-test dev launch ([9455038](https://github.com/kernel-panic-codecave/Archie/commit/9455038782200f4049cfbdbedb8a41077041b133)) - KernelPanic

### Other Changes

- initial commit ([076427c](https://github.com/kernel-panic-codecave/Archie/commit/076427cd2a1bbcd023b045a05f3bf09fa4ab67c9)) - Witherking25
- port to 1.21 and start of UI code ([967dd2f](https://github.com/kernel-panic-codecave/Archie/commit/967dd2f1b96cad69aa3bc43a17cf8caf675a5d89)) - KernelPanic
- changed package name and added test mod ([624bcc6](https://github.com/kernel-panic-codecave/Archie/commit/624bcc653f523fca62b2b6a6b47c9848d1fbd535)) - KernelPanic
- K2 and docs ([816bf01](https://github.com/kernel-panic-codecave/Archie/commit/816bf01caee30896bcdce44e39edc360cc63609d)) - KernelPanic
- ci ([2033a63](https://github.com/kernel-panic-codecave/Archie/commit/2033a63440011159c07d975492cb6772f44eb46b)) - KernelPanic
- token ([2614178](https://github.com/kernel-panic-codecave/Archie/commit/26141782adebdefeb03a6e9b0c9d7e5686f95030)) - KernelPanic
- ci ([97ca981](https://github.com/kernel-panic-codecave/Archie/commit/97ca98125efab54cd2abefc2b0eb9bea11e96113)) - KernelPanic
- ci ([22257a6](https://github.com/kernel-panic-codecave/Archie/commit/22257a6b402290dc4427c73ce577f26a01683d70)) - KernelPanic
- ci ([6a110c2](https://github.com/kernel-panic-codecave/Archie/commit/6a110c242ee0ec41b5a0febbcbd982a1b6795ca6)) - KernelPanic
- Gradle Caching ([4c750cf](https://github.com/kernel-panic-codecave/Archie/commit/4c750cf3f5a9660d2ab9dddb5cd70421961a51db)) - KernelPanic
- Theme ([75272e6](https://github.com/kernel-panic-codecave/Archie/commit/75272e6e0a7507e9892f6b843d3ad8f38ba55ed0)) - KernelPanic
- Versioned Docs and k2 ([cbf71a5](https://github.com/kernel-panic-codecave/Archie/commit/cbf71a5d68fc97b605f1a905a8eeb6f60795a365)) - KernelPanic
- Versioned Docs and k2 ([6298d6b](https://github.com/kernel-panic-codecave/Archie/commit/6298d6b2b47fbd6b86a9b909ac42e786c27713a1)) - KernelPanic
- Merge remote-tracking branch 'origin/1.21.x' into 1.21.x ([f8ee5e4](https://github.com/kernel-panic-codecave/Archie/commit/f8ee5e4520635cfe35e7a9a96b9ed62e428d5a74)) - KernelPanic
- Merge remote-tracking branch 'origin/1.21.x' into 1.21.x ([8537931](https://github.com/kernel-panic-codecave/Archie/commit/8537931f8229c117cdff1ed96809488a151f770a)) - KernelPanic
- Fix Box centering by adding fillMaxSize modifier to root container ([ba6af62](https://github.com/kernel-panic-codecave/Archie/commit/ba6af628ddbd69045375c9193bbdf9c34f2e4137)) - KernelPanic
- Fix Box centering: prevent children from inheriting parent size constraints ([6945129](https://github.com/kernel-panic-codecave/Archie/commit/6945129257ecaab6dd41fd70557345a98d5102cf)) - KernelPanic
- Fix padding and margin handling in Row, Column, and Box measure policies ([05faa76](https://github.com/kernel-panic-codecave/Archie/commit/05faa76ff7a0f8ba5f037e6c9882e0629332cce1)) - KernelPanic
- Add block entity state synchronization system (Phase 1-2) ([e9bf3c0](https://github.com/kernel-panic-codecave/Archie/commit/e9bf3c0dc0bf31dbc59900242aa51e04174ca67e)) - KernelPanic
- Add observable collections, block entity updates, and GUI components ([ad8a8a1](https://github.com/kernel-panic-codecave/Archie/commit/ad8a8a19be0e16f4c074d35ce4feb29802d9dd72)) - KernelPanic
- Merge branch 'chore/archie-reorg-refactor-docs' into 1.21.x ([4130d01](https://github.com/kernel-panic-codecave/Archie/commit/4130d01c9f0510d8f4fb639965436f9d48f2cf94)) - KernelPanic
- neoforge: add gui depth and client threading mixins ([4a9342f](https://github.com/kernel-panic-codecave/Archie/commit/4a9342f04dd7321447ab4894181409604004a720)) - KernelPanic
- so many things ([22e4879](https://github.com/kernel-panic-codecave/Archie/commit/22e48798bcb77d2d2b67beee583270675893ad60)) - KernelPanic
- Add Claude Code GitHub Workflow ([#1](https://github.com/kernel-panic-codecave/Archie/pull/1)) - @KP2048
- fix ci ([0494e11](https://github.com/kernel-panic-codecave/Archie/commit/0494e1100ef458b6dad3aa737cb5c90b9c0599f4)) - KernelPanic
- Add radio/switch textures, fix zero-size widget rendering, and finish composite-build/config cleanup ([25dbd1d](https://github.com/kernel-panic-codecave/Archie/commit/25dbd1d21661a735baeb588d3a6ee3144478b093)) - KernelPanic
- Add CI check workflow and fix the widget-state render bug plus a NeoForge remapJar failure ([4a1f65b](https://github.com/kernel-panic-codecave/Archie/commit/4a1f65b453d82a96d4dc1d960f91fd88ffc27804)) - KernelPanic
- Fix client GameTest runner hanging forever on any test failure ([9ff9225](https://github.com/kernel-panic-codecave/Archie/commit/9ff92252a5372ca18e42565e2581fcc2c8b6e087)) - KernelPanic
- Fix invalid gametest log filenames and improve CI test-failure visibility ([c83e0cd](https://github.com/kernel-panic-codecave/Archie/commit/c83e0cd6b4598b6385cd673bb37e94e3919b6246)) - KernelPanic
- fix build script errors ([601769a](https://github.com/kernel-panic-codecave/Archie/commit/601769ab902d0a5899252a98819d468f975dbfbe)) - KernelPanic
- Add comprehensive UI component GameTests, hierarchy/input test DSL, and a shared widget-state resolver ([d0ea6ab](https://github.com/kernel-panic-codecave/Archie/commit/d0ea6ab2dee4180ef34441a3bd8e76c1bfe38fae)) - KernelPanic
- Fix bugs found by the new UI GameTest suite, dedupe cross-mod test runs, and split CI by loader ([6d5428e](https://github.com/kernel-panic-codecave/Archie/commit/6d5428ec0cc98c3f2dfad444fd069c9286841cdd)) - KernelPanic
- Fix ConcurrentModificationException crash during LayoutNode measure/render ([3004118](https://github.com/kernel-panic-codecave/Archie/commit/300411855d1d7b58b06167b7ec2e0ce6a4afb3d4)) - KernelPanic
- Fix ENTER-never-fires-at-origin hover bug, sync the real cursor with synthetic test input, serialize same-loader GameTest invocations, and settle layout before reading node bounds ([a3f9235](https://github.com/kernel-panic-codecave/Archie/commit/a3f9235ac5e4f536e0b306e83ec2638ae5490c62)) - KernelPanic
- Fix testSwitchHoverAndClickRenderState: use Recomposer.hasPendingWork for isComposeIdle() ([b431a37](https://github.com/kernel-panic-codecave/Archie/commit/b431a37019475b45282c1992b5e7aebae568da3a)) - KernelPanic
- Fix ChoiceDialog disabled-state race and JUnit report job-name misattribution ([d0544b1](https://github.com/kernel-panic-codecave/Archie/commit/d0544b19c60f3278f201bb612ce151b4e646064e)) - KernelPanic
- Tolerate a mod having zero GameTest functions on a given side instead of crashing the invocation ([becd187](https://github.com/kernel-panic-codecave/Archie/commit/becd1877c099f520907582ac69b1076830c826ff)) - KernelPanic
- Fix Phaser double-arrival crash and Compose runtime leak on screen swap ([41da03f](https://github.com/kernel-panic-codecave/Archie/commit/41da03feadeaed9baeb6cfe7dea0a0e9eb2f4aab)) - KernelPanic
- Fix assertRenderState's failure message re-reading live state instead of the compared value ([cfa2a5a](https://github.com/kernel-panic-codecave/Archie/commit/cfa2a5abcc70dc2d1604f5f0cf8c0566917533ac)) - KernelPanic
- Fix Phaser stale-phase-memory bug causing intermittent GameTest stalls ([076e93e](https://github.com/kernel-panic-codecave/Archie/commit/076e93eb67948823e4779b82d40bc3842a2a6022)) - KernelPanic
- fixed config ([4637408](https://github.com/kernel-panic-codecave/Archie/commit/4637408a8fbdf633724d85a18a4097ea1f043e65)) - KernelPanic
- Overhaul config system with client/server sync, and sync docs ([71e2697](https://github.com/kernel-panic-codecave/Archie/commit/71e269747038782858a2785348f3a4c49cd3fd82)) - KernelPanic
- Sync markdown guides and KDoc with the config overhaul's collateral changes ([#5](https://github.com/kernel-panic-codecave/Archie/pull/5)) - @KP2048
- Add ProgressBar/EnergyBar/FluidTank composables and energy storage ([#3](https://github.com/kernel-panic-codecave/Archie/pull/3)) - @KP2048
- Enable per-module Maven publishing, misc build/CI/editor tweaks ([5cacf02](https://github.com/kernel-panic-codecave/Archie/commit/5cacf0248c4a43a119008a3616ad66b91035212b)) - KernelPanic
- release ([24b586a](https://github.com/kernel-panic-codecave/Archie/commit/24b586a5a69c2174ff2cffdce2f87fd0ac47e241)) - KernelPanic

