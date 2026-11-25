# Sleep Timer

App Android che dopo un tempo scelto manda il comando **Pausa** alla musica in riproduzione:
Apple Music, Spotify, YouTube, podcast e qualsiasi app che usa i controlli multimediali standard.
L'app non prende mai il controllo dell'audio: manda solo il comando di pausa.

## Funzioni

- Pulsanti rapidi: 15, 30, 45 min, 1 h e 1 h 30.
- Tempo personalizzato da 1 a 240 minuti, con slider e pulsanti −5 / −1 / +1 / +5.
- L'app ricorda l'ultima durata usata.
- Notifica silenziosa con il conto alla rovescia e i pulsanti **+5 min** e **Cancel**.
- Riquadro nelle impostazioni rapide: un tocco avvia il timer con l'ultima durata, un altro tocco lo annulla. Il riquadro mostra l'ora in cui si fermerà la musica. Una pressione prolungata apre l'app.
- Alla scadenza la musica va in pausa senza suoni né vibrazioni.

## Come manda la pausa

1. **Con l'accesso alle notifiche** (consigliato): l'app trova i player attivi e manda Pausa direttamente a quelli che stanno suonando. Il permesso serve solo per vedere i player attivi; le notifiche non vengono lette.
2. **Senza permesso**: l'app simula il tasto "pausa" delle cuffie. Android lo manda all'app che ha riprodotto audio per ultima, di solito quella giusta.

## Installazione sul Pixel

1. Apri **Android Studio** → *Open* → scegli la cartella `sleep-timer`. Al primo avvio scarica Gradle e le librerie, e ci vogliono alcuni minuti.
   Se Android Studio propone di aggiornare il plugin Android Gradle (AGP), puoi accettare o ignorare: funziona in entrambi i casi.
2. Sul Pixel attiva le **Opzioni sviluppatore**: *Impostazioni → Informazioni sul telefono →* tocca 7 volte *Numero build*.
3. In *Impostazioni → Sistema → Opzioni sviluppatore* attiva **Debug USB**, oppure **Debug wireless**.
4. Collega il telefono, sceglilo in alto in Android Studio e premi ▶ **Run**.

## Attivare la pausa diretta

Nell'app tocca **Allow notification access** e attiva *Sleep Timer*.

Se l'interruttore è grigio ("Impostazione con restrizioni"), Android lo blocca perché l'app non arriva dal Play Store:

1. Tocca **Open app info** nell'app, oppure vai in *Impostazioni → App → Sleep Timer*.
2. Tocca il menu **⋮** in alto a destra → **Consenti impostazioni con restrizioni**.
3. Torna indietro e attiva l'accesso alle notifiche.

## Struttura del codice

| File | Cosa fa |
|---|---|
| `SleepTimer.kt` | Avvia, estende e annulla il timer; usa un allarme esatto, quindi niente servizio in background |
| `MediaPauser.kt` | Manda Pausa: prima alle sessioni attive, altrimenti con il tasto multimediale |
| `TimerNotification.kt` | Notifica con conto alla rovescia, +5 min e Cancel |
| `SleepTileService.kt` | Riquadro delle impostazioni rapide |
| `TimerReceiver.kt` | Riceve scadenza e pulsanti; ripristina il timer dopo un riavvio |
| `MainActivity.kt` | Interfaccia (Jetpack Compose, Material You) |

Per cambiare le durate rapide modifica `QUICK_PRESETS` in `MainActivity.kt`. Per cambiare l'estensione modifica `EXTEND_MINUTES` in `SleepTimer.kt` e il testo `extend` in `res/values/strings.xml`.
