package com.helloauntie.app

import android.content.Context
import com.helloauntie.app.data.PersonRepository
import com.helloauntie.app.domain.AnalysisEngine
import com.helloauntie.app.ml.AndroidFaceDetector
import com.helloauntie.app.ml.DemoPersonDetector
import com.helloauntie.app.ml.ModelAssets
import com.helloauntie.app.ml.TfliteFaceEmbedder
import com.helloauntie.app.ml.TflitePersonDetector
import com.helloauntie.core.face.FaceMatcher
import com.helloauntie.core.kinship.KinshipEngine
import com.helloauntie.core.kinship.Region

object AppContainer {

    @Volatile
    var region: Region = Region.SOUTH

    lateinit var repository: PersonRepository
        private set
    lateinit var analysisEngine: AnalysisEngine
        private set

    fun init(context: Context) {
        if (::repository.isInitialized) return
        val appContext = context.applicationContext
        repository = PersonRepository(appContext)

        val personDetector = if (ModelAssets.exists(appContext, ModelAssets.PERSON)) {
            runCatching { TflitePersonDetector(appContext.assets) }
                .getOrElse { DemoPersonDetector() }
        } else {
            DemoPersonDetector()
        }

        val faceDetector = AndroidFaceDetector()
        val embedder = if (ModelAssets.exists(appContext, ModelAssets.EMBEDDER)) {
            runCatching { TfliteFaceEmbedder(appContext.assets) }.getOrNull()
        } else {
            null
        }

        analysisEngine = AnalysisEngine(
            personDetector = personDetector,
            faceDetector = faceDetector,
            faceEmbedder = embedder,
            faceMatcher = FaceMatcher(),
            galleryProvider = { repository.gallery() },
            labelProvider = { personId ->
                runCatching {
                    val ego = repository.ensureEgo()
                    val person = repository.people().firstOrNull { it.id == personId }
                    if (person == null) {
                        null
                    } else {
                        val term = KinshipEngine(repository.graph(), region).term(ego.id, personId)
                        if (term.known) "${term.primary} ${person.name}" else person.name
                    }
                }.getOrNull()
            },
        )
    }
}
