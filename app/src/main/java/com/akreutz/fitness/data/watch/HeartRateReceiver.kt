package com.akreutz.fitness.data.watch

/**
 * Puts together the heart rate readings the watch sends, piece by piece, after a finished session (see
 * [WatchCommand.HeartRateChunk]).
 */
class HeartRateReceiver {
    /** The watch's complete readings for a session: one byte per second from [recordingStartEpochSeconds]. */
    class Recording(
        val sessionId: String,
        val recordingStartEpochSeconds: Long,
        val samples: ByteArray,
    )

    private var sessionId: String? = null
    private var recordingStartEpochSeconds = 0L
    private var sampleCount = 0
    private var pieces: Array<List<Int>?> = emptyArray()

    /** How many pieces of the recording being received are here so far. */
    val receivedPieces: Int
        get() = pieces.count { it != null }

    /** How many pieces the recording being received has, or 0 when none is on its way. */
    val totalPieces: Int
        get() = pieces.size

    /**
     * Takes in [chunk]. Returns the whole recording once every piece of it has arrived, `null` until then. A watch with
     * no readings sends a single piece that says so, which returns an empty recording. A piece that arrives twice
     * (the watch sends it again when it is unsure it got through) is taken in once.
     */
    fun onChunk(chunk: WatchCommand.HeartRateChunk): Recording? {
        if (chunk.count == 0) return Recording(chunk.sessionId, 0, ByteArray(0))
        if (chunk.sessionId != sessionId || chunk.count != pieces.size || chunk.sampleCount != sampleCount) {
            sessionId = chunk.sessionId
            recordingStartEpochSeconds = chunk.recordingStartEpochSeconds
            sampleCount = chunk.sampleCount
            pieces = arrayOfNulls(chunk.count)
        }
        pieces[chunk.index] = chunk.packed
        if (pieces.any { it == null }) return null

        val samples = ByteArray(sampleCount)
        var next = 0
        for (piece in pieces) {
            for (number in piece.orEmpty()) {
                for (position in 0 until READINGS_PER_NUMBER) {
                    if (next < sampleCount) samples[next++] = (number shr (BITS_PER_READING * position)).toByte()
                }
            }
        }
        val recording = Recording(chunk.sessionId, recordingStartEpochSeconds, samples)
        sessionId = null
        pieces = emptyArray()
        return recording
    }

    private companion object {
        const val READINGS_PER_NUMBER = 3
        const val BITS_PER_READING = 8
    }
}
