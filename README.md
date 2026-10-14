# Fiszki — niemiecki krok po kroku

Aplikacja Compose Multiplatform dla Androida i iOS. Wspólne ekrany, import CSV, baza SQLite i zasady nauki znajdują się w module `shared`. Aplikacja działa lokalnie, bez konta i internetu.

**one-shoted by astra**

Wskazówki do dalszej pracy nad kodem znajdują się w [AGENTS.md](AGENTS.md).

## Uruchomienie

### Android

Otwórz projekt w Android Studio obsługującym AGP 9.2, zsynchronizuj Gradle i uruchom konfigurację `app`. Wymagane: JDK 17 lub nowszy (zalecany JBR z Android Studio), Android SDK 36.1; urządzenie/emulator z Androidem 10 (API 29) lub nowszym.

```powershell
.\gradlew.bat :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

### iOS

Na macOS z Xcode i JDK otwórz `iosApp/iosApp.xcodeproj`, wybierz schemat `Fiszki` i symulator iPhone'a. Faza budowania Xcode uruchamia Gradle i osadza framework `FiszkiShared` wraz z zasobami Compose. Na fizycznym urządzeniu ustaw swój zespół w **Signing & Capabilities**. Minimalna wersja systemu to iOS 15.

Projekt ma targety urządzeń ARM64 oraz symulatorów ARM64 i x64. Budowa iOS wymaga macOS; nie można jej wykonać na Windows. Jeśli Xcode nie widzi JDK, ustaw `JAVA_HOME` dla środowiska budowania. Wyłączenie `User Script Sandboxing` jest już zapisane w projekcie, aby Gradle mógł zbudować framework.

## Słownictwo i baza

Źródłem słownictwa jest **`slowka.csv` w katalogu głównym**: 3390 fraz. Gradle pakuje ten sam plik do zasobów obu aplikacji, bez ręcznego utrzymywania kopii.

```csv
deutsch;polski
der Zoll;cło
die Münze /n;moneta
```

Kodowanie: UTF-8, opcjonalnie BOM. Parser obsługuje separator `;` (oraz przecinek dla innych plików), opcjonalny nagłówek, CRLF/LF, puste wiersze, pola w cudzysłowach, podwójne cudzysłowy i podziały wiersza wewnątrz pola. Niepoprawny lub pusty plik powoduje czytelny błąd i umożliwia ponowienie próby. Wiersze, w tym powtórzenia w oryginale, zachowują kolejność.

Pierwszy start tworzy `fiszki.db` i importuje cały plik w jednej transakcji. Kolejne starty odczytują istniejący postęp. Zmiana CSV nie nadpisuje danych użytkownika. Aby zaimportować zmieniony zestaw podczas prac programistycznych, wyczyść dane aplikacji lub ją przeinstaluj (wyłączenie przywracania kopii może być potrzebne na Androidzie).

Jedna tabela `flashcards`:

| Pole | Znaczenie |
| --- | --- |
| `id` | Stabilny klucz i kolejność w CSV |
| `german` | Fraza niemiecka |
| `polish` | Fraza polska |
| `correct_streak` | Poprawne odpowiedzi z rzędu, od 0 do 5 |

Android przechowuje bazę w prywatnym katalogu baz aplikacji; iOS w Application Support. Operacje SQLite działają poza głównym wątkiem, są serializowane i zamykają połączenia po użyciu.

## Zasady sesji

- Sesja pobiera maksymalnie 20 fraz z `correct_streak < 5`, malejąco po wyniku, a przy remisie rosnąco po `id`.
- Lista jest ustalana na początku sesji. Każda fraza pojawia się raz, najpierw po polsku. Dotknięcie odwraca kartę i ujawnia niemiecki tekst; kolejne dotknięcie odwraca ją z powrotem.
- **Pamiętam** zwiększa wynik o 1, a **Jeszcze nie** zeruje go. Odpowiedź jest zapisywana przed przejściem dalej. Podczas zapisu przyciski są zablokowane, a spóźnione kliknięcia poprzedniej karty są ignorowane.
- Wynik 5 oznacza opanowaną frazę. Nowe frazy uzupełniają listę przy rozpoczęciu kolejnej sesji. Gdy pozostaje mniej niż 20, sesja jest odpowiednio krótsza.
- Ostatnia odpowiedź przenosi na ekran główny z aktualnym postępem i podsumowaniem. Gdy wszystkie frazy są opanowane, pojawiają się gratulacje, a przycisk rozpoczęcia sesji znika.
- Można zakończyć sesję wcześniej. Zapisane odpowiedzi pozostają; ponowne otwarcie aplikacji rozpoczyna od ekranu głównego. Obrót ekranu Androida zachowuje bieżącą sesję dzięki `ViewModel`.

## Sprawdzanie

Testy działają na JVM bez emulatora, używając rzeczywistego SQLite w tymczasowych plikach:

```powershell
.\gradlew.bat :shared:jvmTest :app:assembleDebug
```

Obejmują parser, pełny spakowany CSV, pierwsze 20 fraz, priorytety, reset wyniku, próg opanowania, wymianę fraz, końcową krótką sesję, ponowne otwarcie bazy, błędny import oraz przejście całej sesji z ochroną przed podwójną odpowiedzią.

Ręcznie na obu platformach warto sprawdzić: odwracanie karty, przewijanie długich fraz, duży tekst systemowy, tryb poziomy, przerwanie sesji, ponowne uruchomienie i ekran gratulacji.

Konfiguracja modułów opiera się na [zaleceniach migracji KMP do AGP 9](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html), a baza korzysta z [SQLite dla Kotlin Multiplatform](https://developer.android.com/kotlin/multiplatform/sqlite).

## Wynik weryfikacji (14.10.2026)

- `:app:assembleDebug`: poprawna kompilacja.
- `:shared:jvmTest`: 11 testów, 0 błędów.
- Emulator Pixel 3a, Android 14: sprawdzono ekran startowy, pełny import 3390 fraz, odwrócenie fiszki, sesję 20 odpowiedzi (19 poprawnych, 1 błędna), powrót do ekranu głównego i zachowanie postępu po zamknięciu procesu.
- Przycisk rozpoczęcia sesji jest zawsze dostępny w dolnej części ekranu; treść powyżej można przewijać.
- iOS: dodano projekt Xcode; kompilacja i kontrola interfejsu na iOS pozostają do wykonania na macOS.
