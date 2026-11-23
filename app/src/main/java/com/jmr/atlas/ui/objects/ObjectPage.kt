package com.jmr.atlas.ui.objects

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jmr.atlas.model.*
import com.jmr.atlas.ui.AtlasViewModel
import com.jmr.atlas.ui.cards.*
import com.jmr.atlas.ui.components.Empty

/** Every item opens to a card shaped by what it is (design I01–I12), not a generic tab layout. */
@Composable
fun DetailScreen(vm: AtlasViewModel, snap: Snapshot, id: String) {
    val o = snap.objects[id]
    if (o == null) { Box(Modifier.fillMaxSize()) { Empty("Not found", "This item was deleted.") }; return }
    when (o.type) {
        ObjType.PERSON -> PersonCard(vm, snap, o)
        ObjType.PROJECT -> ProjectCard(vm, snap, o)
        ObjType.TASK -> TaskCard(vm, snap, o)
        ObjType.DOCUMENT, ObjType.IMAGE -> DocumentCard(vm, snap, o)
        ObjType.LINK -> LinkCard(vm, snap, o)
        else -> GenericCard(vm, snap, o)
    }
}
