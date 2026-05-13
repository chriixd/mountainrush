# 🏔️ MOUNTAIN RUSH

App Android **stile Need for Speed** per cronometrare le tue corse sui passi di montagna.

Registra in tempo reale velocità attuale, velocità massima, velocità media, distanza, tempo, **dislivello positivo/negativo**, quota massima e numero di tornanti. Salva ogni run nello storico locale con mappa del percorso, profilo altimetrico ed esportazione GPX.

## ⚙️ Caratteristiche

- 🚗 **Cruscotto live** stile NFS — sfondo nero, neon rosso/arancione
- 📡 GPS in foreground service: continua a registrare anche con schermo spento
- 🗺️ **OpenStreetMap** integrato (zero registrazioni, zero API key)
- ⛰️ Statistiche pensate per i passi: dislivello cumulato, quota max, numero di tornanti
- 📊 Recap con grafico altimetrico stile telemetria
- 💾 Storico locale (database Room) con dettaglio di ogni run
- 📤 Esportazione tracciato GPX per condividerlo su Strava/Komoot/ecc.

## 🏗️ Come ottenere l'APK (zero installazioni locali)

### Passo 1 — Crea la repository GitHub

1. Vai su https://github.com/new
2. Nome repo: `mountainrush` (o quello che preferisci)
3. **Pubblica** o **Private** — entrambi vanno bene
4. NON aggiungere README/gitignore (li ho già messi io)
5. Clicca **Create repository**

### Passo 2 — Carica i file del progetto

Hai 2 opzioni:

**Opzione A — Da interfaccia web (più semplice):**
1. Sulla pagina della repo appena creata, clicca "uploading an existing file"
2. Trascina **tutta la cartella `MountainRush` estratta dallo zip** (o tutti i suoi contenuti)
3. Commit changes
4. ℹ️ Il gradle wrapper viene generato automaticamente dal workflow, non devi preoccupartene

**Opzione B — Da linea di comando (se hai git):**
```bash
cd MountainRush
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/TUO-USERNAME/mountainrush.git
git push -u origin main
```

### Passo 3 — Attendi il build automatico

1. Vai sulla tab **"Actions"** della tua repo
2. Vedrai un workflow "Build APK" in esecuzione (~3-5 minuti la prima volta)
3. Quando diventa verde ✅, clicca sul run
4. Scrolla in fondo, sezione **"Artifacts"**: scarica `MountainRush-release-apk`
5. Estrai lo zip → trovi `app-release.apk`

### Passo 4 — Installa sul telefono

1. Trasferisci `app-release.apk` sul telefono (Drive, USB, Telegram, email)
2. Apri il file sul telefono
3. Android chiederà di abilitare "Installa app da sorgenti sconosciute" — fallo
4. Installa
5. ⚠️ Al primo avvio l'app chiederà permesso **posizione** — concedi "Mentre l'app è in uso"
6. Per registrazione in background con schermo spento, concedi anche **posizione tutto il tempo**

## 📂 Struttura

```
MountainRush/
├── app/
│   ├── build.gradle              # config app (dipendenze)
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # permessi e componenti
│   │   ├── java/com/mountainrush/app/
│   │   │   ├── MountainRushApp.kt   # Application class
│   │   │   ├── data/                # Room DB + modelli (RunSession, TrackPoint)
│   │   │   ├── service/             # LocationTrackingService (foreground) + RunTracker
│   │   │   ├── ui/                  # 4 Activity + ElevationChartView custom
│   │   │   └── util/                # Formatter + GeoUtils (haversine)
│   │   └── res/                     # layout, colori, drawable, tema
├── build.gradle                  # config root
├── settings.gradle
├── gradle/wrapper/               # config wrapper (gradle-wrapper.jar generato dal workflow)
└── .github/workflows/build.yml   # build automatico GitHub Actions
```

## 🛠️ Stack tecnico

- **Kotlin** + **AndroidX** (minSdk 24, targetSdk 34)
- **Room** per il database locale
- **osmdroid** per OpenStreetMap
- **Coroutines** per concorrenza
- **LocationManager** (GPS_PROVIDER) per tracking in foreground service
- **Gradle 8.7** + Android Gradle Plugin 8.2.0

## 💡 Note tecniche

- Filtri anti-rumore GPS: scarta fix con accuracy > 30m, micro-spostamenti < 2m
- Dislivello calcolato solo per variazioni > 2m (filtra oscillazioni quota)
- Tornanti rilevati come cambi di bearing > 60° dopo almeno 15m di tratto rettilineo
- Velocità media calcolata sul **tempo in movimento** (> 2 km/h), così le pause non la sporcano
- Foreground service con `foregroundServiceType="location"` (richiesto da Android 14+)

## 🎨 Tema NFS

Palette in `app/src/main/res/values/colors.xml`. Puoi cambiare i neon a piacere:
- `neon_red` (#FF1F3D) — colore principale racing
- `neon_orange` (#FF6B1F) — secondario
- `neon_yellow` (#FFD93D) — accenti grafico
- `bg_dark` (#0A0A0F) — sfondo

Buoni passi 🏔️🔥
