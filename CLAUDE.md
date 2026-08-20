# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Native Android (Kotlin + Jetpack Compose) chat client for LLMs via OpenRouter. Single Gradle module (`app`), package root `com.dsv.llm_demo`.

## Commands

Use `gradlew.bat` on Windows, `./gradlew` in bash. All commands run from the repo root.

```
gradlew.bat assembleDebug                        # build debug APK
gradlew.bat testDebugUnitTest                     # run local unit tests (JVM, app/src/test)
gradlew.bat testDebugUnitTest --tests "com.dsv.llm_demo.ui.llmchat.LlmChatViewModelTest"   # single unit test class
gradlew.bat testDebugUnitTest --tests "*.LlmChatViewModelTest.sendMessage*"                 # single test method
gradlew.bat connectedDebugAndroidTest             # instrumented Compose UI tests (app/src/androidTest); needs an emulator/device
gradlew.bat ktlintCheck                           # lint check (ktlintFormat to auto-fix)
gradlew.bat detekt                                # static analysis
gradlew.bat mergedCoverageReport                  # combined JaCoCo report (unit + instrumented, com.dsv.llm_demo.ui package only); depends on testDebugUnitTest + createDebugCoverageReport
```

Notes:
- `detekt` is configured to read `config/detekt/detekt.yml`, but that file does not currently exist in the repo — running the `detekt` task as-is will fail until that config is added.
- `mergedCoverageReport` requires a connected emulator/device (it depends on `createDebugCoverageReport`), same as the `coverage.yml` CI workflow which runs it on an `android-emulator-runner` (API 29, Nexus 6 profile).
- `OPENROUTER_API_KEY` must be set in `local.properties` (not committed) — it's read in `app/build.gradle.kts` and injected into `BuildConfig.OPENROUTER_API_KEY`, then added as a bearer token header in `NetworkModule`.

## Architecture

**DI (Hilt).** `LlmDemoApp` is the `@HiltAndroidApp` entry. Modules live under `di/` and `di/module/`:
- `NetworkModule` builds the singleton `OkHttpClient`/`Retrofit`/`LlmService`, pointed at `https://openrouter.ai/api/v1/`, with the API key injected as an `Authorization: Bearer` header.
- `RepositoryModule` binds `LlmRepository` to `LlmRepositoryImpl`.
- `LlmChatModule` (`ListFragmentModule`) contributes `LlmChatFragment` into a Dagger multibinding map keyed by `@FragmentKey`.

**Fragment factory pattern.** Screens are not looked up by class/reflection alone: `AppFragmentFactory` resolves fragments from the `Map<Class<out Fragment>, Provider<Fragment>>` built via `@IntoMap @FragmentKey` bindings, falling back to reflection only if no provider is registered. `MainActivity` pulls this factory out of the Hilt graph via `FragmentFactoryEntryPoint` (an `@EntryPoint` on `SingletonComponent`) and installs it on both the activity's and the `NavHostFragment`'s child `FragmentManager` before `super.onCreate`. When adding a new screen, register it the same way (module + `@FragmentKey`) rather than wiring the fragment manually.

**Chat data flow (MVVM + Flow).**
- `LlmChatViewModel` is an interface (`LlmChatUiState` holds messages, input text, loading flag, selected model/reasoning effort); `LlmChatViewModelImpl` is the `@HiltViewModel` implementation. The fragment receives its view model through a constructor-injected `(Fragment) -> LlmChatViewModel` provider (see `LlmChatModule`), not directly via `by viewModels()`, so it can be swapped/mocked — see `LlmChatFragmentTest`.
- `LlmRepository.streamLlmResponse` (impl in `LlmRepositoryImpl`) prepends a fixed system instruction, builds a `ChatCompletionRequest`, and calls `LlmService.streamMessage` (Retrofit `@Streaming` POST to `chat/completions`) with `stream = true`.
- The response is Server-Sent Events: `LlmRepositoryImpl` reads `responseBody.charStream()` line by line, parses each `data: ...` line as `ChatCompletionChunkResponse` with Gson, and `emit`s each text delta as a `Flow<String>` (`flowOn(Dispatchers.IO)`, terminates on `[DONE]`).
- `LlmChatViewModelImpl.sendMessage` collects that flow and incrementally appends deltas onto a single in-progress assistant `ChatMessage` (matched by a UUID generated up front), so the UI streams token-by-token.

**Live code preview.** The repository's system prompt instructs the model to return runnable web apps as fenced ```html/```js code blocks. `util/CodeExtractor.extractWebCode` pulls all such fences out of a message, concatenates them, and — if the result isn't already a full HTML document — wraps it in an HTML boilerplate that loads React, ReactDOM, Babel standalone, and Tailwind from CDNs (so JSX-only snippets still run). `ui/llmchat/components/CodePreviewDialog` renders the extracted HTML (WebView) so users can preview generated apps/games inline in the chat.

**Reasoning effort handling.** `LlmRepositoryImpl` only forwards `reasoningEffort` on the request when the selected model id contains `"o1"` or `"o3"` and the effort isn't `"none"`; otherwise it's sent as `null`.

## Testing conventions

- Unit tests (`app/src/test`) use MockK + `kotlinx-coroutines-test`, targeting view models/repositories on the JVM (no Android framework).
- Instrumented tests (`app/src/androidTest`) use Compose UI testing (`ui-test-junit4`) and MockK-android; `HiltTestActivity` exists to host fragments/composables under test in isolation. `androidTest/util/Helpers.kt` holds shared test helpers.