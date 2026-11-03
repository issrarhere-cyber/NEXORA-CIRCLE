# NEXORA-X language data

These JSON files are the single source of truth for the Nexora-X language.
The app reads them at start-up. To add your real language:

1. Open `alphabet.json` and add one object per letter inside `"glyphs"`:

   ```json
   { "latin": "a", "symbol": "YOUR_SYMBOL", "name": "optional name" }
   ```

2. Open `numbers.json` and add one object per digit inside `"digits"`:

   ```json
   { "latin": "0", "symbol": "YOUR_SYMBOL" }
   ```

3. Commit the files. The keyboard, translator, lessons and games pick them
   up automatically. No Kotlin code needs to change.

Rules the app follows:
- Symbols are never generated, reordered or modified by the app.
- A character with no entry in the table is kept as-is when translating,
  and the translator reports it as "unmapped".
- If two entries use the same `latin` value, the first one wins and the
  duplicate is reported.
