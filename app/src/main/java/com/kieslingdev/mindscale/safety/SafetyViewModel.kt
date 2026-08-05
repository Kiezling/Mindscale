package com.kieslingdev.mindscale.safety

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieslingdev.mindscale.data.SafetyPlanDao
import com.kieslingdev.mindscale.data.SafetyPlanItem
import com.kieslingdev.mindscale.data.SafetyPlanStep
import com.kieslingdev.mindscale.data.allowsPhone
import com.kieslingdev.mindscale.data.groupedByStep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val EDITOR_STEP_KEY = "safety.editor.step"
private const val EDITOR_ID_KEY = "safety.editor.id"
private const val EDITOR_TEXT_KEY = "safety.editor.text"
private const val EDITOR_PHONE_KEY = "safety.editor.phone"

/** The open add/edit modal. `itemId == null` means a new line. */
data class PlanEditor(
    val step: SafetyPlanStep,
    val itemId: Long?,
    val textDraft: String = "",
    val phoneDraft: String = "",
    val textError: String? = null,
    val phoneError: String? = null,
    val saving: Boolean = false
)

data class SafetyUiState(
    val plan: Map<SafetyPlanStep, List<SafetyPlanItem>> =
        SafetyPlanStep.entries.associateWith { emptyList() },
    val editor: PlanEditor? = null,
    val pendingDeleteId: Long? = null,
    val message: String? = null,
    val readError: String? = null
) {
    val isEmpty: Boolean get() = plan.values.all { it.isEmpty() }
}

/**
 * State for the Safety card (`docs/specs/SPEC-safety-card.md`, D-11, Invariant 1).
 *
 * It takes [SafetyPlanDao] and nothing else. There is deliberately no `EntryDao`,
 * `SleepDao`, `MarkerDao`, `EpisodeSourceDao`, or settings access here, so no later edit
 * can make what this card says depend on a rating, an episode, a streak, or a count
 * without changing this constructor and failing review. MindScale does not assess risk and
 * must not appear to.
 *
 * Nothing is recorded when the card is opened or a resource is tapped (D-8). The only
 * writes are the ones the user asks for.
 */
class SafetyViewModel(
    private val planDao: SafetyPlanDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SafetyUiState(editor = restoreEditor()))
    val uiState: StateFlow<SafetyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            planDao.observeAll()
                .catch { error ->
                    if (error is CancellationException) throw error
                    _uiState.update { it.copy(readError = SafetyCopy.LOAD_FAILED) }
                }
                .collect { items ->
                    _uiState.update { it.copy(plan = items.groupedByStep(), readError = null) }
                }
        }
    }

    /**
     * Restores only the open editor, as four primitives — never the plan itself
     * (D-8, Phase 7 envelope pattern). Someone who typed a line in a hard moment should
     * not lose it to a rotation.
     */
    private fun restoreEditor(): PlanEditor? {
        val stepName: String = savedStateHandle[EDITOR_STEP_KEY] ?: return null
        val step = SafetyPlanStep.entries.firstOrNull { it.name == stepName } ?: return null
        val id: Long = savedStateHandle[EDITOR_ID_KEY] ?: -1L
        return PlanEditor(
            step = step,
            itemId = id.takeIf { it > 0L },
            textDraft = savedStateHandle[EDITOR_TEXT_KEY] ?: "",
            phoneDraft = savedStateHandle[EDITOR_PHONE_KEY] ?: ""
        )
    }

    private fun persistEditor(editor: PlanEditor?) {
        savedStateHandle[EDITOR_STEP_KEY] = editor?.step?.name
        savedStateHandle[EDITOR_ID_KEY] = editor?.itemId ?: -1L
        savedStateHandle[EDITOR_TEXT_KEY] = editor?.textDraft
        savedStateHandle[EDITOR_PHONE_KEY] = editor?.phoneDraft
    }

    private fun setEditor(editor: PlanEditor?) {
        persistEditor(editor)
        _uiState.update { it.copy(editor = editor, message = null) }
    }

    fun retry() = _uiState.update { it.copy(readError = null) }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    fun startAdd(step: SafetyPlanStep) {
        val state = _uiState.value
        val error = when {
            state.plan.values.sumOf { it.size } >= MAX_PLAN_ITEMS_TOTAL -> PlanFieldError.PlanFull
            (state.plan[step]?.size ?: 0) >= MAX_PLAN_ITEMS_PER_STEP -> PlanFieldError.StepFull
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(message = error.message()) }
            return
        }
        setEditor(PlanEditor(step = step, itemId = null))
    }

    fun startEdit(id: Long) {
        val item = _uiState.value.plan.values.flatten().firstOrNull { it.id == id }
        if (item == null) {
            _uiState.update { it.copy(message = SafetyCopy.STALE_ITEM) }
            return
        }
        setEditor(
            PlanEditor(
                step = item.step,
                itemId = item.id,
                textDraft = item.text,
                phoneDraft = item.phone.orEmpty()
            )
        )
    }

    fun updateTextDraft(value: String) {
        val editor = _uiState.value.editor ?: return
        setEditor(editor.copy(textDraft = value, textError = null))
    }

    fun updatePhoneDraft(value: String) {
        val editor = _uiState.value.editor ?: return
        setEditor(editor.copy(phoneDraft = value, phoneError = null))
    }

    fun cancelEdit() = setEditor(null)

    fun saveEditor() {
        val editor = _uiState.value.editor ?: return
        if (editor.saving) return

        val text = when (val result = validatePlanText(editor.textDraft)) {
            is PlanFieldResult.Valid -> result.value
            is PlanFieldResult.Invalid -> {
                setEditor(editor.copy(textError = result.error.message()))
                return
            }
        }
        val phone = when (val result = validatePlanPhone(editor.phoneDraft, editor.step)) {
            is PlanFieldResult.Valid -> result.value
            is PlanFieldResult.Invalid -> {
                setEditor(editor.copy(phoneError = result.error.message()))
                return
            }
        }

        setEditor(editor.copy(saving = true))
        viewModelScope.launch {
            try {
                if (editor.itemId == null) {
                    planDao.addItem(editor.step, text, phone)
                } else if (planDao.updateContent(editor.itemId, text, phone) != 1) {
                    // The row went away between opening the editor and saving. Nothing is
                    // reinserted: silently recreating a line the user deleted elsewhere
                    // would be a mutation they never asked for.
                    setEditor(null)
                    _uiState.update { it.copy(message = SafetyCopy.STALE_ITEM) }
                    return@launch
                }
                setEditor(null)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                // The editor stays open with what was typed, so a retry costs nothing.
                setEditor(editor.copy(saving = false))
                _uiState.update { it.copy(message = SafetyCopy.SAVE_FAILED) }
            }
        }
    }

    fun requestDelete(id: Long) = _uiState.update { it.copy(pendingDeleteId = id, message = null) }

    fun cancelDelete() = _uiState.update { it.copy(pendingDeleteId = null) }

    fun confirmDelete() {
        val id = _uiState.value.pendingDeleteId ?: return
        viewModelScope.launch {
            try {
                val removed = planDao.removeItem(id)
                _uiState.update {
                    it.copy(
                        pendingDeleteId = null,
                        message = if (removed) null else SafetyCopy.STALE_ITEM
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(pendingDeleteId = null, message = SafetyCopy.SAVE_FAILED)
                }
            }
        }
    }

    /** Null when the stored number has nothing dialable left in it (D-7). */
    fun dialActionFor(item: SafetyPlanItem): SafetyAction.Dial? {
        if (!item.step.allowsPhone) return null
        val number = item.phone?.let(SafetyActions::dialString) ?: return null
        return SafetyAction.Dial(number)
    }

    /** Reported when the platform has no app for a hand-off. Nothing is recorded. */
    fun reportActionUnavailable(action: SafetyAction) =
        _uiState.update { it.copy(message = SafetyActions.unavailableMessage(action)) }
}
