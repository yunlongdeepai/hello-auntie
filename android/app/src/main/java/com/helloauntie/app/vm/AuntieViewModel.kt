package com.helloauntie.app.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.helloauntie.app.AppContainer
import com.helloauntie.core.kinship.KinshipEngine
import com.helloauntie.core.kinship.Region
import com.helloauntie.core.kinship.RelationPreset
import com.helloauntie.core.kinship.TermResult
import com.helloauntie.core.model.Gender
import com.helloauntie.core.model.Person
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PersonWithTerm(
    val person: Person,
    val term: TermResult,
    val faceCount: Int = 0,
)

class AuntieViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppContainer.repository

    private val _people = MutableStateFlow<List<PersonWithTerm>>(emptyList())
    val people: StateFlow<List<PersonWithTerm>> = _people.asStateFlow()

    private val _region = MutableStateFlow(Region.SOUTH)
    val region: StateFlow<Region> = _region.asStateFlow()

    private val _selfBirthYear = MutableStateFlow<Int?>(null)
    val selfBirthYear: StateFlow<Int?> = _selfBirthYear.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val ego = repository.ensureEgo()
        _selfBirthYear.value = ego.birthYear
        val persons = repository.people()
        val counts = repository.faceSampleCounts()
        val engine = KinshipEngine(repository.graph(), _region.value)
        _people.value = persons.map { person ->
            PersonWithTerm(
                person = person,
                term = engine.term(ego.id, person.id),
                faceCount = counts[person.id] ?: 0,
            )
        }
    }

    fun addFaceSample(personId: Long, embedding: FloatArray, quality: Float) {
        repository.saveFaceSample(personId, embedding, quality)
        refresh()
    }

    fun setSelfBirthYear(year: Int?) {
        repository.updateSelfBirthYear(year)
        refresh()
    }

    fun addPerson(
        name: String,
        gender: Gender,
        birthYear: Int?,
        birthOrder: Int?,
        preset: RelationPreset,
    ) {
        if (name.isBlank()) return
        repository.addPersonWithChain(
            name = name.trim(),
            gender = gender,
            birthYear = birthYear,
            birthOrder = birthOrder,
            preset = preset,
        )
        refresh()
    }

    fun deletePerson(personId: Long) {
        repository.deletePerson(personId)
        refresh()
    }

    fun clearAll() {
        repository.clearAll()
        refresh()
    }

    fun setRegion(region: Region) {
        AppContainer.region = region
        _region.value = region
        refresh()
    }
}
