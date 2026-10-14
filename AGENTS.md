# Wskazówki dla agentów

Projekt **Fiszki** to aplikacja Compose Multiplatform do nauki niemieckich fraz na Androidzie i iOS. Szczegóły uruchomienia oraz zasady działania są w [README.md](README.md).

## Układ projektu

- `slowka.csv` w katalogu głównym jest jedynym źródłem słownictwa. Gradle pakuje go do zasobów wspólnego modułu. Zachowaj kodowanie UTF-8, kolejność wierszy i dwie kolumny: niemiecką oraz polską.
- `shared/src/commonMain` zawiera interfejs Compose, parser CSV, repozytorium SQLite i sterowanie sesją. Zmiany zasad nauki wprowadzaj tutaj, aby obie platformy działały jednakowo.
- `app` zawiera punkt wejścia Androida. `iosApp` zawiera projekt Xcode i punkt wejścia iOS; `shared/src/iosMain` tworzy kontroler Compose.
- `shared/src/commonTest` i `shared/src/jvmTest` zawierają testy parsera, bazy i sesji.

## Niezmienniki działania

- Pierwszy start importuje cały CSV do jednej tabeli `flashcards` w pojedynczej transakcji. Następne uruchomienia zachowują bazę i postęp użytkownika.
- Sesja wybiera do 20 nieopanowanych fraz, sortując po `correct_streak DESC, id ASC`. Fraza jest opanowana po 5 poprawnych odpowiedziach z rzędu.
- **Pamiętam** zwiększa licznik, a **Jeszcze nie** zeruje go. Odpowiedź musi zostać zapisana przed pokazaniem następnej fiszki. Gdy fraz jest mniej niż 20, sesja obejmuje pozostałe.
- Fraza zaczyna się po polsku; dotknięcie fiszki pokazuje niemiecki odpowiednik. Po ostatniej odpowiedzi wraca ekran główny. Gdy wszystkie frazy są opanowane, pokazuje gratulacje bez przycisku nowej sesji.

## Budowanie i sprawdzanie

Na Windows użyj `./gradlew.bat :shared:jvmTest :app:assembleDebug`. Aplikację iOS buduj na macOS przez `iosApp/iosApp.xcodeproj`. Po zmianach w interfejsie sprawdź ekran startowy oraz obie strony fiszki na urządzeniu lub emulatorze.

Adnotacja projektu: **one-shoted by astra**.
