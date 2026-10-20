# Quote storage and navigation

`QuoteRepository` is the domain API; `RoomQuoteRepository` implements it. No
presentation code or application startup wiring is included in this change.

## Koin integration

Register `quoteDataModule` when the app's Koin startup is added:

```kotlin
startKoin {
    androidContext(application)
    modules(quoteDataModule)
}
```

Resolve `QuoteRepository` from Koin. The database and repository are singletons.
The module uses the application context and stores `wolf_wisdom.db` in Android's
private database directory. No destructive migration fallback is enabled.

## Repository API

- `getCurrentQuote()` initializes the feed or resumes the persisted cursor.
- `nextQuote()` replays the next history entry, or generates one at the frontier.
- `previousQuote()` replays the previous entry, staying put at the earliest entry.
- `observeCurrentQuote()` initializes on collection and observes cursor/saved changes.
- `observeSavedQuotes()` observes saved quotes, ordered by generated quote ID.
- `setSaved(id, isSaved)` persists the flag; an unknown ID throws `IllegalArgumentException`.

All one-shot methods suspend. Flows are cold and emit distinct domain models.
Calling next/previous on an uninitialized feed returns its first random quote.

## Seed and persistence

The bundled `assets/quotes/volchi_citaty_renumbered.txt` contains all 724 supplied
quotes. Number prefixes are stripped during initialization; Room generates IDs.
The source is validated for sequential numbering, nonempty contents and unique
quote text. Every new quote starts with `isSaved = false`, including a SQL default.

Initialization seeds an empty database in a transaction. Existing installations
keep their data: restarting or resolving another repository never reseeds or
resets saves, history, the cursor or cycle progress. Editing the bundled file
later does not update existing databases; future seed updates need an explicit
data migration. Schema version 1 is exported under `app/schemas`.

## Reading sequence

The quote DTO has only `id`, `quote`, and `isSaved`. Three separate tables store
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
