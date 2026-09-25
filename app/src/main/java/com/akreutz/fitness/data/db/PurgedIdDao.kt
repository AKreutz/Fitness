package com.akreutz.fitness.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akreutz.fitness.data.model.PurgedId

@Dao
interface PurgedIdDao {
    /**
     * Ignores a conflict rather than failing, since re-deleting something already tombstoned
     * (which can't normally happen locally, but matters once sync merges tombstones from another
     * device) shouldn't be an error.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(purgedIds: List<PurgedId>)

    @Query("SELECT * FROM purged_ids")
    suspend fun getAll(): List<PurgedId>
}
