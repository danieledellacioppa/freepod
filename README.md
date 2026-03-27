# freepod

Freepod è un'app Android (Jetpack Compose) per ascoltare podcast con un flusso semplice:

1. **Discover/Search** per trovare nuovi show.
2. **Subscribe** per salvarli localmente.
3. **Episode list + Player** basati su feed **RSS**.

---

## Stato attuale del progetto (marzo 2026)

In questo branch il provider di discovery attivo è:

- ✅ **Apple iTunes Search API**

Il provider **Podcast Index** è stato lasciato nel codice ma **disabilitato** a livello di configurazione runtime, così da poter essere riattivato velocemente in futuro senza cambiare UI.

---

## Architettura discovery: come funziona ora

Per mantenere la UI quasi invariata, è stato introdotto un layer di astrazione nel data layer:

- `PodcastDiscoveryService` (interfaccia comune)
- `ItunesSearchService` (implementazione attiva)
- `PodcastIndexService` (implementazione mantenuta come alternativa)
- `DiscoveryProviderConfig` (selettore del provider)

### Flusso tecnico

- `DiscoverScreen` **non conosce** il provider.
- `DiscoverViewModel` usa solo `PodcastDiscoveryService`.
- `MainActivity` costruisce il provider concreto in base a `DiscoveryProviderConfig.ACTIVE_PROVIDER`.

Risultato: si cambia backend discovery senza toccare (o quasi) la UI.

---

## Mappatura dati iTunes → modello app

L'app continua a usare il modello `PodcastSummary` già esistente.

Per iTunes Search API (`https://itunes.apple.com/search`), mappiamo così:

- `collectionId` → `podcastIndexId` (riuso campo esistente)
- `collectionName` → `title`
- `artistName` → `author`
- `artworkUrl600` (fallback `artworkUrl100`) → `imageUrl`
- `primaryGenreName` → `description`
- `feedUrl` → `feedUrl` (campo fondamentale per subscribe + RSS episodes)

> Nota: `feedUrl` è il requisito minimo per poter proseguire con subscribe e fetch episodi via RSS.

---

## "Trending" con iTunes: decisione implementativa

Podcast Index esponeva un endpoint trending dedicato.

iTunes Search API non ha un endpoint trending equivalente nello stesso formato; per mantenere la UX della schermata Discover quando la query è vuota, `ItunesSearchService.trendingPodcasts()` esegue una ricerca fallback con termine generico:

- query di default: `podcast`

Questo permette di preservare il comportamento atteso della schermata senza redesign UI.

---

## Cosa resta invariato

- UI Discover/Search (layout e componenti principali)
- flusso di subscribe locale (`SharedPreferences`)
- fetch episodi da feed RSS
- playback episodi

In altre parole: è cambiato **solo** il provider dati di discovery.

---

## File principali coinvolti

### Nuovi file

- `app/src/main/java/com/forteur/freepod/data/PodcastDiscoveryService.kt`
- `app/src/main/java/com/forteur/freepod/data/ItunesSearchService.kt`
- `app/src/main/java/com/forteur/freepod/data/DiscoveryProvider.kt`

### File modificati

- `app/src/main/java/com/forteur/freepod/data/PodcastIndexService.kt`
  - ora implementa `PodcastDiscoveryService`
- `app/src/main/java/com/forteur/freepod/ui/screens/DiscoverViewModel.kt`
  - dipende da `PodcastDiscoveryService` invece che da `PodcastIndexService`
- `app/src/main/java/com/forteur/freepod/MainActivity.kt`
  - selezione provider tramite `DiscoveryProviderConfig.ACTIVE_PROVIDER`

---

## Come cambiare provider in futuro (guida dettagliata)

### Scenario A — Tornare a Podcast Index (consigliato se vuoi ranking/trending più specifico)

1. Apri:
   - `app/src/main/java/com/forteur/freepod/data/DiscoveryProvider.kt`
2. Modifica:
   - da `DiscoveryProvider.ITUNES`
   - a `DiscoveryProvider.PODCAST_INDEX`
3. Verifica credenziali Podcast Index in `gradle.properties` (o proprietà Gradle equivalenti):
   - `PODCAST_INDEX_KEY`
   - `PODCAST_INDEX_SECRET`
4. Build/run app.
5. Controlla Discover:
   - query vuota usa trending Podcast Index
   - query testuale usa search Podcast Index

### Scenario B — Restare su iTunes (stato attuale)

Lascia:

```kotlin
val ACTIVE_PROVIDER = DiscoveryProvider.ITUNES
```

Non sono richieste API key aggiuntive per iTunes Search API.

### Scenario C — Aggiungere un terzo provider (es. Listen Notes, backend interno, ecc.)

1. Crea una classe che implementa `PodcastDiscoveryService`.
2. Implementa almeno:
   - `trendingPodcasts(maxResults)`
   - `searchPodcasts(query, maxResults)`
3. Mappa i dati verso `PodcastSummary` assicurandoti che `feedUrl` sia valorizzato.
4. Aggiungi un nuovo valore nell'enum `DiscoveryProvider`.
5. Aggiungi il branch di inizializzazione in `MainActivity`.
6. Imposta il nuovo provider in `DiscoveryProviderConfig.ACTIVE_PROVIDER`.

Se rispetti questi punti, la UI Discover non richiede modifiche strutturali.

---

## Perché questa impostazione è utile

- **Rischio basso**: cambio provider senza riscrivere schermate.
- **Rollback rapido**: tornare a Podcast Index è una singola modifica di config + credenziali.
- **Estendibilità**: nuovi provider integrabili via interfaccia comune.
- **Coerenza UX**: l'utente vede sempre la stessa esperienza in app.

---

## Build rapida

```bash
./gradlew :app:assembleDebug
```

Se usi Podcast Index provider, configura prima le proprietà Gradle delle API key.
