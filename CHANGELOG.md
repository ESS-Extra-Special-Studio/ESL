# Changelog

## 1.1.0

Added:
Lobby API (`EslLobby` / `EslLobbyManager` / `EslLobbyEvent`) for reusable server-side party sessions (no packets or GUI).

## 1.0.1

Update by: Extra_Special_K

Added:

- Wave API (`EslWaveSequence` / `EslWaveController` / `EslWaveManager`) so mods can run multi-round defense waves without a third-party wave mod.

Fixed:

- After the last wave in a sequence, the controller completes immediately. It no longer waits the between-wave delay or advances as if another round were starting.

## 1.0.0

Update by: Extra_Special_K

Added:

- ES Library (ESL) foundation: config helpers (`EslConfig`), mod-list helpers (`EslMods`), thread-safe `EslRegistry`, small string utils.
- Backend-only boundary — no GUI or rendering code.
