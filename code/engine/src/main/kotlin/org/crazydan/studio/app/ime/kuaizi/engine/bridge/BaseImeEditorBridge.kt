/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.engine.bridge

import org.crazydan.studio.app.ime.kuaizi.engine.CursorDirection
import org.crazydan.studio.app.ime.kuaizi.engine.EditorEditAction
import org.crazydan.studio.app.ime.kuaizi.engine.TextRange

abstract class BaseImeEditorBridge : ImeEditorBridge {

    protected data class SelectionSnapshot(
        val beforeStart: Int,
        val beforeEnd: Int,
        val beforeContent: String,
        val afterStart: Int,
        val afterEnd: Int,
    )

    private var revertion: SelectionSnapshot? = null

    protected fun resetRevertion() {
        revertion = null
    }

    protected fun recordRevertion(
        beforeStart: Int, beforeEnd: Int, beforeContent: String,
        afterStart: Int, afterEnd: Int,
    ) {
        revertion = SelectionSnapshot(
            beforeStart = beforeStart,
            beforeEnd = beforeEnd,
            beforeContent = beforeContent,
            afterStart = afterStart,
            afterEnd = afterEnd,
        )
    }

    override fun revokeCommit() {
        val snapshot = revertion ?: return
        revertion = null
        onRevokeCommit(snapshot)
    }

    protected abstract fun onRevokeCommit(snapshot: SelectionSnapshot)

    override fun commitText(text: String, replacements: List<String>?) {
        resetRevertion()
        if (replacements != null && replacements.isNotEmpty()) {
            doReplaceableCommitText(text, replacements)
        } else {
            doNormalCommitText(text)
        }
    }

    protected abstract fun doNormalCommitText(text: String)
    protected abstract fun doReplaceableCommitText(text: String, replacements: List<String>)
}
