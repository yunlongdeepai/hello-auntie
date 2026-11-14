package com.helloauntie.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.helloauntie.core.face.FaceTemplate
import com.helloauntie.core.kinship.ChainStep
import com.helloauntie.core.kinship.KinshipGraph
import com.helloauntie.core.kinship.KinToken
import com.helloauntie.core.kinship.RelationPreset
import com.helloauntie.core.model.Gender
import com.helloauntie.core.model.Person
import com.helloauntie.core.model.Relation
import com.helloauntie.core.model.RelationType
import com.helloauntie.core.model.SiblingAge
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PersonRepository(context: Context) {

    private val helper = AuntieDbHelper(context.applicationContext)
    private val db: SQLiteDatabase get() = helper.writableDatabase

    fun ensureEgo(): Person {
        queryPerson("is_self = 1")?.let { return it }
        val id = insertPerson(
            name = "我",
            gender = Gender.UNKNOWN,
            birthYear = null,
            birthOrder = null,
            isSelf = true,
        )
        return Person(id, "我", Gender.UNKNOWN, isSelf = true)
    }

    fun updateSelfBirthYear(year: Int?) {
        val values = ContentValues().apply {
            if (year == null) putNull("birth_year") else put("birth_year", year)
        }
        db.update("person", values, "is_self = 1", null)
    }

    fun people(): List<Person> {
        val result = mutableListOf<Person>()
        db.query(
            "person",
            null,
            null,
            null,
            null,
            null,
            "is_self DESC, name COLLATE LOCALIZED ASC",
        ).use { cursor ->
            while (cursor.moveToNext()) result += cursor.toPerson()
        }
        return result
    }

    fun relations(): List<Relation> {
        val result = mutableListOf<Relation>()
        db.query("relation", null, null, null, null, null, "id ASC").use { cursor ->
            while (cursor.moveToNext()) {
                result += Relation(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    fromId = cursor.getLong(cursor.getColumnIndexOrThrow("from_id")),
                    toId = cursor.getLong(cursor.getColumnIndexOrThrow("to_id")),
                    type = RelationType.fromStorage(cursor.getString(cursor.getColumnIndexOrThrow("type"))),
                    siblingAge = SiblingAge.fromStorage(cursor.getString(cursor.getColumnIndexOrThrow("sibling_age"))),
                    confidence = cursor.getDouble(cursor.getColumnIndexOrThrow("confidence")),
                )
            }
        }
        return result
    }

    fun graph(): KinshipGraph = KinshipGraph(people(), relations())

    fun addPersonWithChain(
        name: String,
        gender: Gender,
        birthYear: Int?,
        birthOrder: Int?,
        preset: RelationPreset,
    ): Long {
        val ego = ensureEgo()
        var current = ego
        var target: Person? = null

        preset.steps.forEachIndexed { index, step ->
            val isLast = index == preset.steps.lastIndex
            val person = if (isLast) {
                val id = insertPerson(name, gender, birthYear, birthOrder, isSelf = false)
                Person(id, name, gender, birthYear, birthOrder, isSelf = false)
            } else {
                findOrCreateIntermediate(current, step)
            }
            insertRelationForStep(current, person, step)
            current = person
            if (isLast) target = person
        }
        return target?.id ?: ego.id
    }

    fun deletePerson(personId: Long) {
        val person = queryPerson("id = $personId") ?: return
        if (person.isSelf) return
        db.beginTransaction()
        try {
            db.delete("relation", "from_id = ? OR to_id = ?", arrayOf(personId.toString(), personId.toString()))
            db.delete("face_sample", "person_id = ?", arrayOf(personId.toString()))
            db.delete("person", "id = ?", arrayOf(personId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearAll() {
        db.beginTransaction()
        try {
            db.delete("relation", null, null)
            db.delete("face_sample", null, null)
            db.delete("person", null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        ensureEgo()
    }

    fun saveFaceSample(personId: Long, embedding: FloatArray, quality: Float) {
        val buffer = ByteBuffer.allocate(embedding.size * 4).order(ByteOrder.nativeOrder())
        embedding.forEach(buffer::putFloat)
        val values = ContentValues().apply {
            put("person_id", personId)
            put("embedding", buffer.array())
            put("quality", quality)
            put("created_at", System.currentTimeMillis())
        }
        db.insert("face_sample", null, values)
    }

    fun gallery(): List<FaceTemplate> {
        val result = mutableListOf<FaceTemplate>()
        db.query("face_sample", arrayOf("person_id", "embedding"), null, null, null, null, "id ASC").use { cursor ->
            while (cursor.moveToNext()) {
                val personId = cursor.getLong(0)
                val bytes = cursor.getBlob(1) ?: continue
                if (bytes.size % 4 != 0) continue
                val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
                val embedding = FloatArray(bytes.size / 4) { buffer.float }
                result += FaceTemplate(personId, embedding)
            }
        }
        return result
    }

    fun faceSampleCounts(): Map<Long, Int> {
        val result = mutableMapOf<Long, Int>()
        db.rawQuery(
            "SELECT person_id, COUNT(*) AS n FROM face_sample GROUP BY person_id",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result[cursor.getLong(0)] = cursor.getInt(1)
            }
        }
        return result
    }

    fun addSocialPerson(
        name: String,
        gender: Gender,
        birthYear: Int?,
        socialTitle: String,
    ): Long = insertPerson(
        name = name,
        gender = gender,
        birthYear = birthYear,
        birthOrder = null,
        isSelf = false,
        socialTitle = socialTitle,
    )

    private fun insertPerson(
        name: String,
        gender: Gender,
        birthYear: Int?,
        birthOrder: Int?,
        isSelf: Boolean,
        socialTitle: String? = null,
    ): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("gender", gender.toStorage())
            if (birthYear == null) putNull("birth_year") else put("birth_year", birthYear)
            if (birthOrder == null) putNull("birth_order") else put("birth_order", birthOrder)
            put("is_self", if (isSelf) 1 else 0)
            if (socialTitle == null) putNull("social_title") else put("social_title", socialTitle)
            put("created_at", System.currentTimeMillis())
        }
        return db.insertOrThrow("person", null, values)
    }

    private fun insertRelationForStep(current: Person, person: Person, step: ChainStep) {
        val relation = when (step.token) {
            KinToken.F, KinToken.M -> Relation(
                fromId = person.id,
                toId = current.id,
                type = RelationType.PARENT,
            )
            KinToken.S, KinToken.D -> Relation(
                fromId = current.id,
                toId = person.id,
                type = RelationType.PARENT,
            )
            KinToken.H, KinToken.W -> Relation(
                fromId = current.id,
                toId = person.id,
                type = RelationType.SPOUSE,
            )
            KinToken.OB, KinToken.OS, KinToken.LB, KinToken.LS, KinToken.B, KinToken.Z -> Relation(
                fromId = current.id,
                toId = person.id,
                type = RelationType.SIBLING,
                siblingAge = step.siblingAge,
            )
        }
        val values = ContentValues().apply {
            put("from_id", relation.fromId)
            put("to_id", relation.toId)
            put("type", relation.type.toStorage())
            put("sibling_age", relation.siblingAge.toStorage())
            put("confidence", relation.confidence)
        }
        db.insertWithOnConflict("relation", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun findOrCreateIntermediate(current: Person, step: ChainStep): Person {
        when (step.token) {
            KinToken.F, KinToken.M -> {
                findParent(current.id, step.gender)?.let { return it }
            }
            KinToken.H, KinToken.W -> {
                findSpouse(current.id)?.let { return it }
            }
            KinToken.S, KinToken.D -> {
                findChild(current.id, step.gender)?.let { return it }
            }
            KinToken.OB, KinToken.OS, KinToken.LB, KinToken.LS, KinToken.B, KinToken.Z -> {
                findSibling(current.id, step.gender, step.siblingAge)?.let { return it }
            }
        }
        val id = insertPerson(
            name = step.label,
            gender = step.gender,
            birthYear = null,
            birthOrder = null,
            isSelf = false,
        )
        return Person(id, step.label, step.gender)
    }

    private fun findChild(parentId: Long, gender: Gender): Person? {
        val sql = """
            SELECT p.* FROM person p
            INNER JOIN relation r ON r.to_id = p.id
            WHERE r.from_id = ? AND r.type = 'PARENT' AND p.gender = ?
            LIMIT 1
        """.trimIndent()
        db.rawQuery(sql, arrayOf(parentId.toString(), gender.toStorage())).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toPerson() else null
        }
    }

    private fun findSibling(personId: Long, gender: Gender, age: SiblingAge): Person? {
        val forwardAge = age.toStorage()
        val reverseAge = age.inverted().toStorage()
        val sql = """
            SELECT p.* FROM person p
            INNER JOIN relation r ON (
                (r.from_id = ? AND r.to_id = p.id) OR
                (r.to_id = ? AND r.from_id = p.id)
            )
            WHERE r.type = 'SIBLING' AND p.gender = ?
              AND (
                (r.from_id = ? AND r.sibling_age = ?) OR
                (r.to_id = ? AND r.sibling_age = ?)
              )
            LIMIT 1
        """.trimIndent()
        db.rawQuery(
            sql,
            arrayOf(
                personId.toString(),
                personId.toString(),
                gender.toStorage(),
                personId.toString(),
                forwardAge,
                personId.toString(),
                reverseAge,
            ),
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toPerson() else null
        }
    }

    private fun findParent(childId: Long, gender: Gender): Person? {
        val sql = """
            SELECT p.* FROM person p
            INNER JOIN relation r ON r.from_id = p.id
            WHERE r.to_id = ? AND r.type = 'PARENT' AND p.gender = ?
            LIMIT 1
        """.trimIndent()
        db.rawQuery(sql, arrayOf(childId.toString(), gender.toStorage())).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toPerson() else null
        }
    }

    private fun findSpouse(personId: Long): Person? {
        val sql = """
            SELECT p.* FROM person p
            INNER JOIN relation r ON (r.from_id = p.id OR r.to_id = p.id)
            WHERE r.type = 'SPOUSE' AND (r.from_id = ? OR r.to_id = ?) AND p.id != ?
            LIMIT 1
        """.trimIndent()
        db.rawQuery(sql, arrayOf(personId.toString(), personId.toString(), personId.toString())).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toPerson() else null
        }
    }

    private fun queryPerson(selection: String): Person? {
        db.query("person", null, selection, null, null, null, "id ASC", "1").use { cursor ->
            return if (cursor.moveToFirst()) cursor.toPerson() else null
        }
    }

    private fun Cursor.toPerson(): Person = Person(
        id = getLong(getColumnIndexOrThrow("id")),
        name = getString(getColumnIndexOrThrow("name")),
        gender = Gender.fromStorage(getString(getColumnIndexOrThrow("gender"))),
        birthYear = getColumnIndexOrThrow("birth_year").let { if (isNull(it)) null else getInt(it) },
        birthOrder = getColumnIndexOrThrow("birth_order").let { if (isNull(it)) null else getInt(it) },
        isSelf = getInt(getColumnIndexOrThrow("is_self")) == 1,
        socialTitle = getColumnIndexOrThrow("social_title").let {
            if (isNull(it)) null else getString(it)?.takeIf { value -> value.isNotBlank() }
        },
    )
}
