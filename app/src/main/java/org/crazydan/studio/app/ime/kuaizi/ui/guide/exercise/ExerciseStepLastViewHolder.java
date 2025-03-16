/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
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

package org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise;

import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.KeyImageRender;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-19
 */
public class ExerciseStepLastViewHolder extends ExerciseStepViewHolder {
    private final Button restartBtn;
    private final Button continueBtn;

    public ExerciseStepLastViewHolder(@NonNull View itemView) {
        super(itemView);

        this.restartBtn = itemView.findViewById(R.id.btn_restart);
        this.continueBtn = itemView.findViewById(R.id.btn_continue);
    }

    /** 视图与数据的初始绑定 */
    @Override
    public void bind(KeyImageRender keyImageRender, ExerciseStep.ViewData data, int position) {
        super.bind(keyImageRender, data, position);

        whenViewReady(this.restartBtn, (btn) -> {
            ViewUtils.visible(btn, data.restartCallback != null);

            btn.setOnClickListener(data.restartCallback == null //
                                   ? null : (v) -> data.restartCallback.run());
        });

        whenViewReady(this.continueBtn, (btn) -> {
            ViewUtils.visible(btn, data.continueCallback != null);

            btn.setOnClickListener(data.continueCallback == null //
                                   ? null : (v) -> data.continueCallback.run());
        });
    }
}
