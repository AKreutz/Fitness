package com.akreutz.fitness.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A tombstone recording that the row with [id] (a [TrainingPlan], [Workout], [Exercise],
 * [WorkoutSession], or [ExercisePerformanceRecord]) was permanently deleted, at [purgedAt]. Grows
 * only — rows are never removed from this table — so future multi-device sync can tell "this id
 * was deleted" apart from "this id was never seen yet", and drop it from a merge even if the
 * other device's copy looks newer. Deleting a row that cascades to others (e.g. a [TrainingPlan]
 * cascading to its [Workout]s) tombstones every cascaded-away id too, since SQLite's own cascade
 * delete happens below Room and emits no per-row callback to hook into.
 */
@Entity(tableName = "purged_ids")
data class PurgedId(
    @PrimaryKey
    val id: String,
    val purgedAt: Instant = Instant.now(),
)
