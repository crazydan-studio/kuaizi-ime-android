package org.crazydan.studio.app.ime.kuaizi.engine.input_action

interface InputActionPositionResolver {
    fun resolve(key: String): OffsetF?
    fun resolveCandidatePosition(index: Int): OffsetF?
    fun resolveInputItemPosition(index: Int): OffsetF?
}
