package com.akreutz.fitness.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.akreutz.fitness.data.model.WeightStack
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightStackDao {
    @Insert
    suspend fun insert(weightStack: WeightStack)

    @Update
    suspend fun update(weightStack: WeightStack)

    @Delete
    suspend fun delete(weightStack: WeightStack)

    @Query("SELECT * FROM weight_stacks ORDER BY name")
    fun observeAll(): Flow<List<WeightStack>>

    @Query("SELECT * FROM weight_stacks")
    suspend fun getAll(): List<WeightStack>

    /** Inserts or updates each of [weightStacks] by id. Used to write a synced merge back. */
    @Upsert
    suspend fun upsertAll(weightStacks: List<WeightStack>)
}
