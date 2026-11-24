# Hub and sub-page pattern (Epic 17), for reuse by Epic 14

How the `Larva` page became a hub with closable children in Scelight's navigation tree, and how to build the same thing again (for example for the player stats window). Evidence and the full Q1 to Q8 answers are in [epic17-subpages-spike.md](epic17-subpages-spike.md); the batch worker numbers are in [epic17-batch-spike.md](epic17-batch-spike.md); how to drive it from an agent is in [running-scelight.md](running-scelight.md). This note only states the rules and points at the code (package `hu.aleks.larvasecondslostextmod`).

## 1. Module-owned `IPage`, not `guiFactory.newPage`

- Implement `IPage` yourself: `LarvaHubPage` (hub, not closeable) and `LarvaSubPage` (child, closeable, `kind` = `SINGLE_REPLAY` or `MULTI_ANALYSIS`). Factory pages (`newPage(..., true, ...)`) also work, but their name is immutable and you cannot ask them for their type, so only use them as a fallback.
- Add the hub once at startup: `services.getMainFrame().getMultiPageComp().addPage( hub )` then `rebuildPageTree( false )` (`LarvaPageNavigator.install()`).
- Add a child: `child.setParent( hub ); hub.getChildList().add( child ); rebuildPageTree( false ); if ( select ) selectPage( child );`. The hub auto-expands.
- Pages with `getChildList() == null` are leaves. **Sub-pages must be leaves**: closing a page that has children throws `UnsupportedOperationException` in `MultiPageComp.removeSelectedPage`. The hub's `closeable` is false (X disabled, Ctrl+F4 does nothing).

## 2. The child list is the registry

The host removes a closed page from `hub.getChildList()` by itself, so there is no second list to keep in sync. Lookups ("is this replay already open?", "how many replay pages are open?") scan the child list: `LarvaHubPage.findSingleReplayPage` (pure, unit-tested), `LarvaPageNavigator.findSingleReplayPage`. Dedupe key = `Path.toAbsolutePath().normalize()` (`LarvaReplayPaths.normalize`); the label is only the file name, so two replays with the same name get the same label.

## 3. Lazy components: keep state in a controller, not in the component

Scelight calls `createPageComp()` only when the page is first selected, then caches the component. A page added with `select == false` has **no component** until the user clicks it. Therefore:

- All page state lives in a per-page **controller** that exists from the moment the page is added: `LarvaReplayPageController` (BUSY / READY / ERROR, result, request id) and `LarvaMultiAnalysisPageController` (RUNNING / DONE / CANCELLED / FAILED, progress, result). `LarvaSubPage` holds it.
- The component (`LarvaReplayPageComp`, `LarvaMultiAnalysisPageComp`) only renders the controller's current state when it is created, and is told about later changes through a small view interface (`LarvaReplayPageView`, `LarvaMultiAnalysisView`). Results that arrive before the component exists are stored and shown on first selection (this is why auto-opened pages need no re-analysis).
- A page created without selection must not touch Swing components.

## 4. Rename

`page.setDisplayName( name ); rebuildPageTree( false );` updates the tree row and, for the selected page, the title bar. Without the rebuild nothing changes. Used for `Multi analysis (running…)` then `from <start> to <end>` (`LarvaMultiPageLabels`, `LarvaPageNavigator.relabel`). The tree is not auto-sized (the divider width stays), so keep tree labels short and put the long text in the page header.

## 5. `pageSelected()` is re-fired on every rebuild: be idempotent

`rebuildPageTree` fires `pageSelected()` again on the **currently selected** page's component. Every add, rename or close of any page calls it. So `pageSelected` must be cheap and must never start work (no analysis, no auto-load). The Larva components do not implement it at all (hub: AC-1.4; replay page: no auto-latest; multi page: renders from the controller).

## 6. Close and cleanup

- The host owns close (X in the title bar, `Ctrl+F4`, same handler). `pageClosing()` may veto (return `false`); Larva does not.
- `pageDisposed()` is the cleanup hook, and it **always** runs for a closed page (a page that never got a component cannot be closed, because closing acts on the selected page, which creates the component). Implement `IPageDisposedListener` on the component and call `controller.dispose()`: it cancels the run, drops the result, and ignores late EDT callbacks. Log lines to look for: `[larva-replay] page disposed, pending results are dropped: <path>`, `[larva-multi] page disposed, run cancelled, pending callbacks are dropped (N files)`, `cancelled after k/N`, `late progress callback ignored, page closed`.
- After closing the last child the hub is selected (previous page from history otherwise).

## 7. Threading and EDT rules

- All page-tree work (`addPage`, `rebuildPageTree`, `selectPage`, `setDisplayName`, hub child list changes) and every Swing update runs on the EDT. Mutating the child list from another thread is unsupported. `LarvaPageNavigator` guards its public methods with `requireEdt` / `requireReady` and logs instead of throwing.
- `guiUtils.runInEDT` is `invokeAndWait` off the EDT: never call it under a lock. Post from workers with `SwingUtilities.invokeLater`.
- **Never parse or analyze on the EDT or on Scelight's `NewReplayHandlerJob` thread** (the thread that calls `INewRepListener`). The listener records the path and hands off to a queue, then returns (0.06 to 0.5 ms measured).

## 8. Worker and queue pattern

- Single replays: one daemon thread `larva-replay-loader` (`LarvaSingleReplayAnalysisQueue`), FIFO, owns the only `ReplaySummaryService`; results are posted with `invokeLater`; queued work of closed pages is skipped. Manual and auto-open requests share it. Auto-open: pending counter with CAS (`LarvaAutoOpenPendingLimit`), decisions in the pure `LarvaAutoOpenPolicy` (qualify, select, cap, admit), EDT admission re-check before the page is created.
- Multi analysis: one single-use `LarvaMultiAnalysisRunner` per page, daemon thread `larva-multi-analysis` at priority NORM-1, its own source/service created on that thread (the service holds a `SimpleDateFormat` and an analyzer: not thread-safe, **one service per worker**), cancellation token checked per file, progress throttled to 100 ms with a final flushed post, results (view model) built on the worker, EDT only paints.
- A seam for tests at every boundary (`RunFactory`, `LarvaAggregationSource`, `LarvaClock`, `Executor`, `Confirmer`), so controllers, pipeline and runner are unit-tested without Scelight.
- Never keep the full per-replay analysis report: store a compact record per game (D-8), about 1 KB.

## 9. Testing it without a mouse

The dev command channel (E17-S00, `-Dlarva.dev.commands.file=<file>`) drives the real Scelight: `dump-tree`, `select`, `close` (clicks the host's own close button, so it is the X handler), `open-replay`, `multi-open`, `multi-page-state`, `auto-open-submit`, `show-help` and more, all listed in [running-scelight.md](running-scelight.md). Every story that adds a user action adds its command. Real clicks, modal dialogs, the real key press for `Ctrl+F4` and look and feel remain short manual checks.

## 10. Reuse checklist (Epic 14 and others)

1. Own `IPage` class per page kind, children as leaves, hub not closeable.
2. State in a controller; component renders it lazily; view interface for updates.
3. `pageDisposed()` cancels and drops.
4. `rebuildPageTree( false )` after every add and rename; `pageSelected()` idempotent.
5. Navigator class that owns all tree mutation, EDT-guarded, with the dedupe lookup.
6. Workers never on the EDT; one service per worker; throttle progress.
7. Add the dev commands and a `knowhow` line for each new action.
