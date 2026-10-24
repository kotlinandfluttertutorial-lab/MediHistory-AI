# Skill: Android Development for MediHistory AI

## Purpose

Guide consistent, safe Android development across MediHistory AI. Covers Kotlin, Jetpack Compose, MVVM, Hilt, Room, StateFlow/Flow, Kotlin Coroutines, CameraX, WorkManager, EncryptedSharedPreferences, and Android Keystore.

## When to Apply This Skill

- Adding or modifying any Android screen, ViewModel, or composable
- Wiring up a new data source (Room DAO, API client, file store)
- Implementing background work (OCR processing, sync, alert generation)
- Handling camera capture or photo picking
- Storing any data locally (preferences, files, database)

## Steering Files to Read First

- `.kiro/steering/tech.md`
- `.kiro/steering/structure.md`
- `.kiro/steering/testing.md`

---

## Step-by-Step Guidance

### 1. Define the Repository Interface First

Before writing any data-access code, declare the interface in the domain layer. The ViewModel depends on the interface; the concrete Room/API implementation is injected by Hilt.

### 2. Model UI State as a Sealed Class

Every screen has exactly one `UiState` sealed class with `Loading`, `Success`, and `Error` variants. The ViewModel exposes it as `StateFlow<UiState>`; composables collect it and branch on the variant.

### 3. Launch Coroutines from `viewModelScope`

All async work in a ViewModel uses `viewModelScope.launch { … }`. Never create your own `CoroutineScope` inside a ViewModel or hold a reference to a `Job` longer than one operation.

### 4. Keep Composables Stateless Where Possible

Composables accept a `Modifier` and lambdas for events. They read from state hoisted to the caller or collected from a ViewModel. They do not hold their own `MutableState` unless the state is purely local UI (e.g., expand/collapse).

### 5. Confine File I/O to App-Private Storage

Any file written at runtime (captured images, downloaded documents, exports) goes to `context.getFilesDir()` or `context.cacheDir()`. Never write to `Environment.getExternalStorageDirectory()` or any path accessible to other apps.

### 6. Encrypt All Sensitive Local Data

- Preferences that contain tokens, keys, or user settings: use `EncryptedSharedPreferences`.
- Database encryption: evaluate SQLCipher/Room-Keystore integration before production and document the recommendation.
- Encryption keys: always generated and stored in Android Keystore. Never store raw key bytes in SharedPreferences or files.

### 7. Define Navigation Routes as Constants

All Compose Navigation destinations are declared as constants in a `sealed class Routes`. No raw strings in `navController.navigate(…)` calls.

### 8. Background Work via WorkManager

Long-running or deferrable operations (OCR, sync, alert generation) are implemented as `CoroutineWorker` subclasses managed by WorkManager. Do not use `Thread`, `AsyncTask`, or fire-and-forget coroutines from an Activity.

---

## Patterns to Follow

- Always define a `Repository` interface; inject the interface, not the implementation.
- ViewModels hold UI state as `StateFlow<UiState>`.
- `UiState` is a sealed class with `Loading`, `Success`, `Error` variants.
- Use `viewModelScope.launch` for coroutines in ViewModel.
- Room queries return `Flow<T>` for reactive updates.
- All file I/O goes to app-private storage only.
- All encryption keys come from Android Keystore.
- Composables accept a `Modifier` parameter and are stateless where possible.
- Navigation routes are constants in a `sealed class Routes`.
- Inject `OcrService` by its interface — never import a provider SDK directly.

## Patterns to Avoid

- Using `Activity` directly in a ViewModel (causes memory leaks).
- Hardcoded hex colours or `dp` values outside design tokens / theme.
- Exposing `MutableStateFlow` directly from a ViewModel — always expose `StateFlow`.
- Storing sensitive data in plaintext `SharedPreferences`.
- Writing files to external shared storage.
- Importing a provider-specific OCR SDK directly into Android code — use the `OcrService` interface.
- Creating a `CoroutineScope` manually inside a ViewModel.

---

## Example Code Structure (Patterns Only — Not Full Implementations)

```kotlin
// ── Repository interface (domain layer) ──────────────────────────────────────
interface PrescriptionRepository {
    fun listByPatient(patientId: String): Flow<List<Prescription>>
    suspend fun getById(id: String): Prescription?
    suspend fun insert(prescription: Prescription)
    suspend fun delete(id: String)
}

// ── UI state ─────────────────────────────────────────────────────────────────
sealed class PrescriptionUiState {
    object Loading : PrescriptionUiState()
    data class Success(val prescriptions: List<Prescription>) : PrescriptionUiState()
    data class Error(val message: String) : PrescriptionUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
@HiltViewModel
class PrescriptionViewModel @Inject constructor(
    private val repository: PrescriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PrescriptionUiState>(PrescriptionUiState.Loading)
    val uiState: StateFlow<PrescriptionUiState> = _uiState.asStateFlow()

    fun load(patientId: String) {
        viewModelScope.launch {
            repository.listByPatient(patientId)
                .catch { e -> _uiState.value = PrescriptionUiState.Error(e.message ?: "Unknown error") }
                .collect { items -> _uiState.value = PrescriptionUiState.Success(items) }
        }
    }
}

// ── Stateless composable ──────────────────────────────────────────────────────
@Composable
fun PrescriptionListScreen(
    uiState: PrescriptionUiState,
    onSelect: (Prescription) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is PrescriptionUiState.Loading -> LoadingIndicator(modifier)
        is PrescriptionUiState.Success -> PrescriptionList(uiState.prescriptions, onSelect, modifier)
        is PrescriptionUiState.Error   -> ErrorMessage(uiState.message, modifier)
    }
}

// ── Navigation routes ─────────────────────────────────────────────────────────
sealed class Routes(val path: String) {
    object PrescriptionList   : Routes("prescriptions")
    object PrescriptionDetail : Routes("prescriptions/{id}") {
        fun withId(id: String) = "prescriptions/$id"
    }
    object AddPrescription    : Routes("prescriptions/add")
}

// ── WorkManager worker skeleton ───────────────────────────────────────────────
class OcrWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val ocrService: OcrService,          // interface, not provider SDK
    private val repository: PrescriptionRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prescriptionId = inputData.getString(KEY_PRESCRIPTION_ID) ?: return Result.failure()
        // update status, call ocrService, persist result — no direct cloud SDK calls here
        return Result.success()
    }

    companion object {
        const val KEY_PRESCRIPTION_ID = "prescription_id"
    }
}
```

---

## Key Constraints (from Steering)

- Medical records are sensitive: encrypt at rest, enforce app-private storage, never log content.
- OCR/AI output is untrusted input: validate and store confidence + review state.
- Cloud backup is opt-in: show visible status and explain retention implications.
- Use synthetic data in all tests and demos; never use real patient data.
