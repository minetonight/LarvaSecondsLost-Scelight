# Epic 17 / E17-S01 spike: module-owned hub page with closable, renamable children

Verified on 2026-11-24 in the real Scelight 6.5.4 (launcher 3.1.4), module built against SDK 1.5.2, driven through the dev command channel (E17-S00). Screenshots: `epic17-spike-hub-with-children.png`, `epic17-spike-child-renamed.png`.

## Result: GO for nesting, GO for rename

Every question below is answered "yes" with evidence from the log or a screenshot. The R-1 contingency (top-level `Larva: <name>` pages) is **not needed**. The R-2 contingency (`Multi analysis #k` label) is **not needed**: renaming works, so S14 can show the dated label after completion (FR-7, OQ-7).

## Spike code (throwaway, delete or promote in S04)

All in package `hu.aleks.larvasecondslostextmod`, prefix `LarvaSpike`:

| Class | Role |
|---|---|
| `LarvaSpikeHubPage` | Module-owned, non-closeable `IPage`, always non-null mutable `ArrayList` child list, content delegated to the unchanged Larva replay page creator. **It now replaces the `guiFactory.newPage("Larva", ...)` page in `installReplayPage()`** (same content, verified by screenshot). |
| `LarvaSpikePage` | Module-owned closeable `IPage` with a mutable display name and a lazily created child list. |
| `LarvaSpikePageComp` | Trivial `JPanel` implementing `IPageSelectedListener`, `IPageClosingListener`, `IPageDisposedListener`; logs `[larva-spike] event ...` lines. |
| `LarvaSpikeCounters` | Per-page lifecycle counters (`created`, `selected`, `closing`, `disposed`), kept after close. |
| `LarvaSpikeUiProbe` | Read-only Swing hierarchy lookup of Scelight's close/back/forward buttons, title label, tree and the Ctrl+F4 action (matched by tool tip text). Lets an agent drive the real host handlers without a mouse. |
| `LarvaSpikeCommands` | The `spike-*` dev commands, registered from `LarvaDevBuiltinCommands.registerAll`. |

Working pattern (mirrors Scelight's `RepAnalyzersPage.newRepAnalyzerPage`), all on the EDT:

```java
child.setParent( hub ); hub.getChildList().add( child );   // our own list
mpc.rebuildPageTree( false );
if ( select ) mpc.selectPage( child );
// rename: page.setDisplayName( "..." ); mpc.rebuildPageTree( false );
// close: host does it (X / Ctrl+F4); it removes the page from hub.getChildList() itself
```

## Answers

**Q1. Hub as module-owned `IPage` renders as a tree node, selectable, shows its own component with children?** Yes. Empty list: `Larva` node as before, `dump-tree` type `LarvaSpikeHubPage`, closable=false, content identical to the old page (screenshot `epic17-spike-hub-with-children.png` shows the normal replay header, buttons and timeline while the hub has three children). With children the hub node shows the expand arrow and is still selectable.

**Q2. Added child is indented and selected, hub auto-expanded?** Yes. Before: `Larva ... expanded=false`; after `spike-add-child alpha own select`: `row 3 text='Larva' expanded=true`, `row 4 depth=3 text='alpha' selected=true`, title bar `alpha`, X enabled. The hub expands automatically (`rebuildPageTree` expands all rows).

**Q3. Module-owned `IPage` children, and `guiFactory.newPage(..., true, ...)` children?** Both work. `beta` (factory page, closeable) added with `child.setParent(hub)` + `hub.getChildList().add(...)`: shown, selected, closed by X with `pageClosing`/`pageDisposed` called. **Use module-owned `LarvaSpikePage`-style pages**: only they allow rename (factory names are immutable) and expose the page type for `dump-tree`/dedupe. `newPage` pages work as a fallback only.

**Q4. Lazy `createPageComp()`? Cached across rebuilds? Child added without selection?**
- Lazy: yes. `spike-add-child gamma own noselect` while `beta` was selected: `counters 'gamma' (created=0 selected=0)`. After `spike-select gamma`: `created=1 selected=1`.
- Cached: yes. `alpha` stayed at `created=1` through every later add/rename/close rebuild (`rebuildPageTree(false)`); only `selected` grew.
- Not selected on add: the tree node appears, the current selection and title stay (`selectedBefore='beta' selectedAfter='beta'`), no component exists. **S06/S07: page state (path, summary, busy/error, job) must live in a controller owned by the page, not in the component**; the component is created on first selection and must render the current controller state then.

**Q5. Close.**
- X button (real `closeAction` via `doClick` on the host's close button): `'beta' -> 'burst1-1'`, `counters 'beta' (created=1 selected=3 closing=1 disposed=1)`. Tree row gone, i.e. the host removed it from **our** child list. Same for a module-owned page (`burst1-1`, Ctrl+F4 action: `closing=1 disposed=1`).
- Next selection: previous page from history (`beta` closed -> `burst1-1`; `burst1-1` closed -> `alpha-rebuilt`). With history cleared (`clearHistory()`) and the last child closed, the hub is selected (`... long label here' -> 'Larva'`), hub collapsed, content intact.
- Veto: `pageClosing()` returning false keeps the page (`closing=1 disposed=0`, still selected, still in the tree). Usable by S14 for "analysis running" confirmation.
- Hub: X disabled (`closeEnabled=false`, grey X in `epic17-spike-hub-with-children.png`); the Ctrl+F4 action does nothing on the hub (it checks the button's enabled state).
- Page with children: `spike-click close` on `burst1-3` that had child `grandkid` logged `java.lang.UnsupportedOperationException at MultiPageComp.removeSelectedPage(MultiPageComp.java:469)`, nothing changed (page still selected, still in the tree). Confirms the hub must stay non-closeable and **sub-pages must be leaves** (no grandchildren).
- Note: a page that never had a component cannot be closed (the X works on the selected page, selecting creates the component), so `pageDisposed()` is always called for a closed page.

**Q6. Rename.**
- Selected page: `spike-rename burst2-2 "Multi 2026-11-10 to 2026-11-24 (selected)"` then `rebuildPageTree(false)`: tree row text and title bar both show the new text (screenshot `epic17-spike-child-renamed.png`), selection unchanged (`selectedBefore == selectedAfter`).
- Unselected page: tree row updated (`gamma` -> `gamma-renamed (unselected)`), title bar of the selected page unchanged, selection unchanged.
- The rebuild **re-fires `pageSelected()` on the currently selected page's component** (`burst2-2 selected=1 -> 2`; `gamma selected` grew with every rebuild while it was current). Every add/rename/close anywhere calls `pageSelected()` again on the current page. **`pageSelected()` must be idempotent and cheap** (no analysis restart, no auto-load).
- Without the rebuild the title bar is not updated (per `MultiPageComp` source it is only set in the selection listener; not separately screenshot-verified); always call `rebuildPageTree(false)` after a rename.
- Long labels: the navigation tree is not auto-sized (`isNavTreeAutoResize()=false`, `getNavTreeMaxAutoWidth()=300` on this machine), so a label such as `Multi 2026-11-10 to 2026-11-24 (selected)` is cut off in the tree (horizontal scroll bar); the title bar shows it in full. `resizeNavTree()` did not change this. S14 should keep the tree label short (for example `Multi 2026-11-10 - 11-24`) and put the full text in the page header.

**Q7. Icons.** Yes. Tree rows render the page's `IRIcon` at 16 px height (`icon=18x16` for the larva icon, 16x16 for the factory/other icons); the title bar shows it at the title font size (`24x24`/`27x24` at the default size). `MultiPageComp` uses `ricon.size(max(16, fontSize))` for both, so larger fonts scale the icon. Per-type icons work: `larva-replay-icon.png` (own pages) and `larva-multi-analysis-icon.png` (factory page `beta`, darker) are distinguishable in the tree. Not tested: a non-default page font size setting (code path read, no special handling needed).

**Q8. Races.**
- Added while the user is on another page: selection and title stay unless `selectPage` is requested.
- Two or more in quick succession: `spike-add-burst 3 noselect` and `spike-add-burst 2 select` post the adds with `SwingUtilities.invokeLater` from a worker thread. All were added in order, with selection staying on `gamma` for the first burst and ending on the last added page for the second (`burst added burst2-1 selected='burst2-1'`, `burst2-2 selected='burst2-2'`). No exceptions.
- Rule: mutate the hub child list only on the EDT (the host reads it on the EDT during rebuild). A list change from another thread is unsupported.

## Downstream effect (outcome table)

| Row | Applies? | Effect |
|---|---|---|
| GO (Q1, Q2, Q3, Q5 OK) | **Yes** | S04: `LarvaHubPage` + `LarvaSubPage` as module-owned `IPage`s as specified. Promote `LarvaSpikeHubPage`/`LarvaSpikePage` (rename), delete `LarvaSpike*` commands/probe when `select`/`close` commands land. |
| NO-GO on nesting | No | S04/S06/S07/S14 keep "under `Larva`". |
| Rename does not work | **No, rename works** | S14: dated label after completion via `setDisplayName` + `rebuildPageTree(false)`, placeholder label while running. Keep the tree label short (long labels are cut off in the nav tree); full text in the header. AC-7.x test the tree label and title bar. |
| Lazy component creation confirmed | **Yes** | S06/S07/S14 keep page state in a controller independent of the component; D17-c (auto-open without selection) works: node appears, nothing created, selection unchanged. |

Extra rules for S04/S06/S07/S14: sub-pages are leaves; `pageSelected()` runs on every rebuild; `pageClosing()` can veto; `pageDisposed()` is the cleanup hook (cancel jobs, unregister from the navigator) and must also remove the page from the navigator registry because the host removes it from our child list itself.

## Dev commands added by the spike (all run on the EDT)

| Command | Effect |
|---|---|
| `spike-add-child <name> [own\|factory] [select\|noselect] [parentLabel]` | Add a child under the hub (or under `parentLabel`, own pages only), rebuild, optionally select |
| `spike-add-burst <n> [select\|noselect]` | n adds posted via `invokeLater` from a worker thread |
| `spike-rename <label> <newName> [norebuild]` | Rename an own page, then `rebuildPageTree(false)` |
| `spike-select <label>` | `selectPage` |
| `spike-click <close\|back\|forward\|prev\|next>` | `doClick` on the real host button (same handler as the mouse); reports disabled buttons |
| `spike-ctrl-f4` | Runs the host's registered Ctrl+F4 action (found in the ancestor input map) |
| `spike-veto <label> <true\|false>` | Make `pageClosing()` veto |
| `spike-clear-history`, `spike-resize` | `IMultiPageComp.clearHistory()`, `resizeNavTree()` |
| `spike-state` | Title text and icon, close/back/forward enabled, every tree row as rendered (text, icon size, expanded, selected), lifecycle counters |

## Manual checks for a human (cannot be done by an agent)

- [ ] Click the **X** in the title bar on a child: page closes, previous page from history is selected, no exception in the log.
- [ ] Press **Ctrl+F4** with the focus inside a child page (the agent only fired the registered action, not the key event): same result. With the hub selected nothing happens.
- [ ] Click the **Back** and **Forward** history buttons after closing children: they never select a closed page (the agent verified this with `doClick`, no mouse).
- [ ] Click a child node in the tree with the mouse and drag the tree divider: labels longer than the divider are cut off (expected).
- [ ] Look at the icons at a larger "Page title font size" / "Page list font size" setting.
