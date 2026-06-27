package org.crazydan.studio.app.ime.kuaizi.ui.viewmodel

sealed class LayoutZone {
    data object A : LayoutZone()
    data object B : LayoutZone()
}
