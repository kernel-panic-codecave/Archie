---
name: sync-docs-after-overhaul
description: Bring KDoc comments and the markdown guides under Archie/docs/ back in sync after a significant refactor or architecture overhaul in this repo (renamed classes, restructured hierarchies, new/removed APIs, changed method signatures). Use this whenever the user says they "overhauled", "refactored", "restructured", or "rewrote" a system and asks to update docs, or whenever you notice staged/unstaged changes that rename or gut core classes in an area that has a doc guide (config, events, gui, networking, registries, resource-packs, serialization, transfer). Don't reach for this for a small fix or single-file change — it's for the case where the shape of a system changed enough that existing docs now describe something that no longer exists.
---

# Sync docs after an overhaul

Docs rot the moment the code they describe changes shape. This skill is the pass that closes that
gap: find every place the old shape is still described, and rewrite it to match what's actually
there now. It applies to any feature area in this repo (config, GUI, networking, serialization,
whatever), not just one — the steps below are the same regardless of which system moved.

## 1. Scope the overhaul from git, not from memory

Start with `git status` and `git diff` / `git diff --cached` (overhauls often land partly staged,
partly not — check both) over the area in question. Renames, new files, and files that shrank
to near-nothing (a class gutted and its contents moved elsewhere) are the signal of a structural
change, as opposed to files that just picked up a few lines. Build a mental list of: what's new,
what's renamed, what moved, what's gone.

## 2. Read the new code directly — never trust the old docs as a starting point

Old docs describe the old shape by definition, so treat them as a *list of claims to verify*, not
as scaffolding to edit in place. Read the actual current source of every changed class: base
classes, what got promoted/demoted in the hierarchy, what parameters got added or renamed, what
new mechanisms appeared (a new sync/networking path, a new required top-level object, a new enum
of variants, etc.). Write down the accurate architecture in your own words before touching any
doc file — if you can't explain the new shape correctly, you can't fix a doc to describe it.

If the class carries its own KDoc, don't assume it's already correct just because it's inside the
changed file — authors update behavior and forget the example in the doc comment above it. Read it
critically, the same as an external markdown guide.

## 3. Enumerate every doc location that could be stale

Check all of these, not just the obvious one:

- **The dedicated markdown guide**, if this feature area has one — `Archie/docs/config.md`,
  `events.md`, `gui.md`, `networking.md`, `registries.md`, `resource-packs.md`,
  `serialization.md`, `transfer.md`. Find it with `ls Archie/docs/`; don't guess a name.
- **`Archie/docs/index.md`**, which has a one-line feature-overview table entry per area — usually
  just needs a phrase added/adjusted, rarely a rewrite.
- **Class- and member-level KDoc** in the overhauled files themselves, *and* in any other file that
  references the overhauled types (grep for the old and new type names across
  `Archie/common/src/main/kotlin` and the loader modules to catch call sites whose doc comments
  now describe stale behavior).
- **Grep the whole repo for old names** (`grep -rn OldClassName`) to catch stragglers a targeted
  read would miss — renames especially leave orphaned references in comments that don't affect
  compilation and so never surface as errors.
- **README.md / AGENTS.md** — usually just point at `Archie/docs/`, so low priority, but check if
  either names a specific API that changed.
- **Archie-Test** — the dev-playground module sometimes demonstrates a feature area directly; check
  whether it does before ruling it out.

A useful trick: the codebase's own best usage example is often more trustworthy than a doc
someone wrote by hand. `Archie.kt` in particular exercises most of Archie's own systems on itself
(its own config, its own registries, etc.) and tends to get kept current because it must compile.
When picking an example for a rewritten guide, prefer lifting a real, current, compiling usage
over inventing a new one from scratch — it can't drift from the code because it *is* the code.

## 4. Rewrite, don't patch around the edges

Once you know the accurate new shape, rewrite the stale sections to match it exactly — class names,
constructor/method signatures, parameter lists, and described behavior. Don't leave a doc in a
half-updated state that mixes old and new terminology (e.g. one section says `CategorySpec` where
the type is now `DataSpec` in that context) — a reader can't tell which parts to trust once that
happens, so an inconsistent doc is often worse than a stale-but-internally-consistent one.

For KDoc, watch for two failure modes:
- A class/member with *no* KDoc at all — new types added during the overhaul are often left
  undocumented entirely. If the class is a central, non-obvious piece of the new architecture, add
  a doc comment; don't limit the pass to only fixing wrong things.
- KDoc `[SomeType]` reference links that no longer resolve because the referenced type was renamed
  or the doc comment lives in a file that doesn't import it — Kotlin doesn't error on these, so they
  silently rot. Prefer fully-qualified links or backtick-quoted plain text over an unresolved
  bracket link when the referenced type isn't already imported in that file.

## 5. Compile after editing KDoc

Doc-comment edits can still break the build — a stray `/*` inside a KDoc block comments out the
rest of the file, mismatched brackets in a code fence, etc. After editing source files (not needed
for markdown-only edits), run the relevant compile task, e.g.:

```
./gradlew :Archie:common:compileKotlin -q
```

Treat a clean compile as confirmation the edits are syntactically sound — it says nothing about
whether the prose is accurate, which is on you from step 2.

## 6. Report back concisely

Summarize what was updated (which files, what kind of change — rewrite vs. small addition vs. new
KDoc added) and flag anything you found but deliberately left alone (e.g. "README only points at
docs/, nothing area-specific to fix there"). Don't produce a separate report document unless asked
— a short end-of-turn summary is enough.
