package com.kieslingdev.mindscale.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * The six Stanley-Brown Safety Planning Intervention steps.
 *
 * **Declaration order is the contract.** It is the canonical ordering from Stanley &
 * Brown (2012) and nothing may reorder, merge, or omit a step
 * (`docs/specs/SPEC-safety-card.md`, D-4). Ordering is always done in Kotlin against
 * [SafetyPlanStep.entries]; it can never be done with `ORDER BY step` in SQL, because the
 * step is stored as its name and alphabetical order is not canonical order.
 */
enum class SafetyPlanStep {
    WARNING_SIGNS,
    INTERNAL_COPING,
    DISTRACTION,
    PEOPLE_FOR_HELP,
    PROFESSIONALS,
    ENVIRONMENT_SAFETY
}

/**
 * Only the two steps that name a person may carry a phone number (D-5). A number on any
 * other step is invalid input, not a value to be silently dropped.
 */
val SafetyPlanStep.allowsPhone: Boolean
    get() = this == SafetyPlanStep.PEOPLE_FOR_HELP || this == SafetyPlanStep.PROFESSIONALS

/**
 * One line of the user's own safety plan, written in advance and stored only on this
 * device. `position` is 0-based and contiguous within a step.
 */
@Entity(tableName = "safety_plan_items")
data class SafetyPlanItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val step: SafetyPlanStep,
    val position: Int,
    val text: String,
    val phone: String? = null
)

class SafetyPlanConverters {
    @TypeConverter
    fun fromSafetyPlanStep(value: SafetyPlanStep): String = value.name

    @TypeConverter
    fun toSafetyPlanStep(raw: String): SafetyPlanStep = SafetyPlanStep.valueOf(raw)
}

/**
 * Groups a flat plan into the canonical six steps, in canonical order, each ordered by
 * position. Pure so the ordering contract is covered by a JVM test rather than by trusting
 * a query.
 */
fun List<SafetyPlanItem>.groupedByStep(): Map<SafetyPlanStep, List<SafetyPlanItem>> =
    SafetyPlanStep.entries.associateWith { step ->
        filter { it.step == step }.sortedWith(compareBy({ it.position }, { it.id }))
    }
