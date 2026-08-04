package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.MarkerDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMarkerDao : MarkerDao {

    private var nextId = 1L
    private val markersFlow = MutableStateFlow<List<Marker>>(emptyList())
    val insertCalls = mutableListOf<Marker>()
    val deleteByIdCalls = mutableListOf<Long>()
    var deleteByIdError: Throwable? = null

    override suspend fun insert(marker: Marker): Long {
        val id = nextId++
        val stored = marker.copy(id = id)
        insertCalls += stored
        markersFlow.value = markersFlow.value + stored
        return id
    }

    override fun observeBetween(fromTs: Long?, toTsExclusive: Long?): Flow<List<Marker>> =
        markersFlow.map { list ->
            list.filter { (fromTs == null || it.ts >= fromTs) && (toTsExclusive == null || it.ts < toTsExclusive) }
                .sortedWith(compareByDescending<Marker> { it.ts }.thenByDescending { it.id })
        }

    override fun observeCount(): Flow<Int> = markersFlow.map { it.size }

    override suspend fun deleteById(id: Long): Int {
        deleteByIdCalls += id
        deleteByIdError?.let { throw it }
        if (markersFlow.value.none { it.id == id }) return 0
        markersFlow.value = markersFlow.value.filterNot { it.id == id }
        return 1
    }
}
