# Quote storage and navigation

`QuoteRepository` is the domain API; `RoomQuoteRepository` implements it.

## Koin integration

`WolfWisdomApplication` registers `quoteDataModule` and `mainModule` on startup:

```kotlin
startKoin {
    androidContext(application)
    modules(quoteDataModule, mainModule)
}
```

Resolve `QuoteRepository` from Koin. The database and repository are singletons.
The module uses the application context and stores `wolf_wisdom.db` in Android's
private database directory. No destructive migration fallback is enabled.

`MainViewModel` observes the current domain quote and populates the screen's
`quote` and `isQuoteSaved` fields. Vertical swipes across the main screen drive the
quote card's rotation. Releasing after a quarter-screen drag (140–200dp) commits
navigation and completes the turn; shorter or cancelled drags ease back without
touching the repository. Swiping down at the beginning only tilts the card and
does not change history. The heart toggles the current quote's saved flag in Room
using its domain ID. The card updates after a successful write; repeated taps and
navigation are blocked during that write to keep quote identity and saved state
consistent. Saves persist when navigating away or restarting the app.

## Repository API

- `getCurrentQuote()` initializes the feed or resumes the persisted cursor.
- `nextQuote()` replays the next history entry, or generates one at the frontier.
- `previousQuote()` replays the previous entry, staying put at the earliest entry.
- `hasPreviousQuote()` reports whether the persisted cursor has an earlier entry.
- `observeCurrentQuote()` initializes on collection and observes cursor/saved changes.
- `observeSavedQuotes()` observes saved quotes, newest `savedAt` first, with quote ID
  as a deterministic tie-breaker. Older saves with unknown dates follow dated saves.
- `setSaved(id, isSaved)` persists the flag and UTC epoch-millisecond save time.
  Repeating a save preserves its timestamp; unsaving clears it, and saving again
  assigns a new timestamp. An unknown ID returns `Result.Failure(DataError.Local.NOT_FOUND)`.

All one-shot methods suspend and return `Result<T, DataError.Local>` (`EmptyResult` for saves).
Flows are cold and emit distinct `Result<T, DataError.Local>` values. An upstream
initialization or observation failure emits `Failure` and completes the flow;
collect again to retry. Cancellation is rethrown, and exceptions from collectors
are not converted to repository failures. Seed IO and validation failures map to
`SEED_READ_FAILED` and `INVALID_SEED_DATA`; SQLite failures map to `DATABASE_ERROR`
(or `DISK_FULL` when storage is full). Other exceptions map to `UNKNOWN`.
Calling next/previous on an uninitialized feed returns its first random quote.

## Seed and persistence

The bundled `assets/quotes/volchi_citaty_renumbered.txt` contains 718 quotes after
removing seven unsuitable entries and adding the owner's quote about the tsar.
Number prefixes are stripped during initialization; Room generates IDs.
The source is validated for sequential numbering, nonempty contents and unique
quote text. Every new quote starts with `isSaved = false`, including a SQL default.

Initialization seeds an empty database in a transaction. Existing installations
keep their data: restarting or resolving another repository never reseeds or
resets saves, history, the cursor or cycle progress. Editing the bundled file
later does not update existing databases; future seed updates need an explicit
data migration. Schemas are exported under `app/schemas`. Migration 1 to 2 adds
nullable `savedAt`, preserving all existing quotes, saved flags, and reading history.
Historical save dates remain unknown rather than being fabricated.

Migration 3 to 4 removes the seven withdrawn quotes by exact text, including their
history and cycle entries. An affected cursor moves to the closest surviving
earlier history entry, or the next entry if there is no earlier one. All other
quote IDs, saved flags, timestamps and history entries remain unchanged. Widgets
bound to removed quotes use the existing unavailable-quote fallback on refresh.

Migration 4 to 5 inserts the new quote only if absent, preserving existing IDs,
saved flags and navigation state. Empty databases are left for normal full seeding.

## Reading sequence

The quote DTO has `id`, `quote`, `isSaved`, `savedAt`, and `isOnWidget`. Three separate tables store
the append-only reading history, its current cursor, and the IDs generated in
the active cycle.

For `A -> B -> C`, going back to B then forward returns C. This does not append
history or affect cycle progress. Only advancing beyond C generates another
quote. The random query excludes every quote already generated in the cycle;
each quote therefore appears exactly once per cycle. After exhaustion, a new
cycle starts without repeating the immediately preceding quote (unless the
dataset contains only one quote). Old history remains available across cycles.

Initialization, navigation and saving use Room transactions, so concurrent
requests cannot interleave cursor changes or consume the same unread entry.
Cancellation or failure rolls back the operation. The cursor, cycle and history
are persisted together and survive closing or force-stopping the app. Uninstalling
or clearing app data resets them. History is intentionally not truncated.

References: [Room asynchronous queries](https://developer.android.com/training/data-storage/room/async-queries),
[Koin Android startup](https://insert-koin.io/docs/reference/koin-android/start/).

Widget membership uses schema version 3. Migration 2 to 3 adds non-null `isOnWidget` with a false default, preserving quote content, favorites and reading history. Widget lifecycle reconciliation derives this flag from active launcher widget IDs and their committed quote bindings. Multiple widgets can share a quote; deleting or rebinding one only clears membership when no active widget still uses that quote. Reconciliation runs at application startup and after binding changes, repairing stale flags after process death or upgrade. The DAO updates changed rows only to avoid Room invalidation loops. Widget status is independent of `isSaved` and `savedAt`.
