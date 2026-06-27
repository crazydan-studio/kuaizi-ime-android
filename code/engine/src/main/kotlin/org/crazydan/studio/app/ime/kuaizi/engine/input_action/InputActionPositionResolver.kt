package org.crazydan.studio.app.ime.kuaizi.engine.input_action

import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey

interface InputActionPositionResolver {
    fun resolve(key: InputKey): OffsetF?
    fun resolveCandidatePosition(index: Int): OffsetF?
    fun resolveInputItemPosition(index: Int): OffsetF?
}
