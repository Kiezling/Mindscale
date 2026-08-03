package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.MarkerDao

class FakeMarkerDao : MarkerDao {

    private var nextId = 1L
    val insertCalls = mutableListOf<Marker>()

    override suspend fun insert(marker: Marker): Long {
        val id = nextId++
        insertCalls += marker.copy(id = id)
        return id
    }
}
