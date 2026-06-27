package org.crazydan.studio.app.ime.kuaizi.engine.domain

import org.crazydan.studio.app.ime.kuaizi.engine.PageDirection

class CandidateListPager {
    fun page(candidateList: CandidateList, direction: PageDirection): CandidateList {
        return when (direction) {
            PageDirection.Next -> candidateList.nextPage()
        }
    }
}
