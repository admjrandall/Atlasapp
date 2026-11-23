package com.jmr.atlas.integrations

import com.jmr.atlas.model.KObject
import com.jmr.atlas.model.Snapshot
import com.jmr.atlas.model.Suggestion

/**
 * Everything Atlas asks of a language model, behind one interface (design 0.7).
 *
 * Atlas's own instant code answers first; a Brain only refines or handles what code can't (general questions,
 * conversation, writing, reading photos). Today the only Brain is [Ai] — Gemma 4 E2B on this phone via LiteRT-LM.
 * The same interface takes Apple's Foundation Models on iPhone, or the user's own server later, without touching
 * the rest of the app. A Brain never acts: everything it returns is shown, and changes go through Atlas's code.
 */
interface Brain {
    /** Shown in the Atlas header so it's always clear where thinking happens, e.g. "On this phone". */
    val where: String
    fun ready(): Boolean

    /** A general answer or a conversational reply. [turns] = earlier exchange, true = the person. */
    suspend fun chat(turns: List<Pair<Boolean, String>>, question: String): String
    /** A short answer about the person's own items, citing them by number. */
    suspend fun answer(question: String, items: List<KObject>, snap: Snapshot): Ai.Answer
    suspend fun nameBands(project: String, bands: List<List<String>>): List<String?>
    suspend fun extract(text: String, snap: Snapshot): List<Suggestion>
    suspend fun summarize(text: String): String
}
