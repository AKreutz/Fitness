package com.akreutz.fitness.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.akreutz.fitness.data.model.WorkoutSession
import com.akreutz.fitness.data.model.WorkoutSessionWithWorkout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Insert
    suspend fun insert(session: WorkoutSession)

    @Delete
    suspend fun delete(session: WorkoutSession)

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY completedAt DESC")
    fun observeAllMostRecentFirst(): Flow<List<WorkoutSessionWithWorkout>>

    @Query("SELECT * FROM workout_sessions WHERE workoutId = :workoutId")
    suspend fun getForWorkout(workoutId: String): List<WorkoutSession>

    /**
     * The [WorkoutSession.workoutId] of the most recently completed session logged against any
     * workout in [trainingPlanId], or `null` if none has been completed yet (or they've all since
     * been deleted). Used to derive which workout to offer next, so deleting a session is
     * reflected immediately rather than needing separate bookkeeping.
     */
    @Query(
        "SELECT workout_sessions.workoutId FROM workout_sessions " +
            "JOIN workouts ON workouts.id = workout_sessions.workoutId " +
            "WHERE workouts.trainingPlanId = :trainingPlanId " +
            "ORDER BY workout_sessions.completedAt DESC LIMIT 1",
    )
    fun observeLastFinishedWorkoutId(trainingPlanId: String): Flow<String?>
}
