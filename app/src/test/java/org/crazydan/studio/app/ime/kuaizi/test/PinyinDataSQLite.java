/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2023 Crazydan Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.crazydan.studio.app.ime.kuaizi.test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinTree;

/**
 * 用于在单元测试中创建存储到 sqlite 中的内置拼音数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-24
 */
public class PinyinDataSQLite {
    private final static String[] word_meta_columns = new String[] {
            "value_", "stroke_count_", "stroke_order_", "simple_word_id_"
    };
    private final static String[] pinyin_meta_columns = new String[] {
            "value_", "chars_", "weight_", "word_id_"
    };

    private final Map<String, PinyinDict.Word> existWordMap = new HashMap<>();
    private final Map<String, PinyinTree.Pinyin> existPinyinMap = new HashMap<>();

    private Connection connection;
    private PreparedStatement wordInsertStatement;
    private PreparedStatement wordUpdateStatement;
    private PreparedStatement pinyinInsertStatement;
    private PreparedStatement pinyinUpdateStatement;

    public void open(File file) throws Exception {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

        prepareSQLite();
    }

    public void close() throws Exception {
        this.connection.close();
    }

    public void withBatch(BatchCaller caller) throws Exception {
        try {
            startBatch();

            caller.call();
        } finally {
            endBatch();
        }
    }

    private void startBatch() throws Exception {
        Date start = new Date();
        queryAllWords().forEach(word -> {
            this.existWordMap.put(word.getValue(), word);
        });
        queryAllPinyins().forEach(pinyin -> {
            this.existPinyinMap.put(pinyin.getValue() + ":" + pinyin.getWord(), pinyin);
        });

        Date end = new Date();
        System.out.println("Reloading consumes: " + (end.getTime() - start.getTime()) + "ms");

        this.wordInsertStatement = this.connection.prepareStatement(String.format(
                "insert into pinyin_word_meta (%s) values (%s)",
                String.join(", ", word_meta_columns),
                Arrays.stream(word_meta_columns).map(c -> "?").collect(Collectors.joining(", "))));
        this.wordUpdateStatement = this.connection.prepareStatement(String.format(
                "update pinyin_word_meta set %s where id_ = ?",
                Arrays.stream(word_meta_columns).map(c -> c + " = ?").collect(Collectors.joining(", "))));

        this.pinyinInsertStatement = this.connection.prepareStatement(String.format(
                "insert into pinyin_pinyin_meta (%s) values (%s)",
                String.join(", ", pinyin_meta_columns),
                Arrays.stream(pinyin_meta_columns).map(c -> "?").collect(Collectors.joining(", "))));
        this.pinyinUpdateStatement = this.connection.prepareStatement(String.format(
                "update pinyin_pinyin_meta set %s where id_ = ?",
                Arrays.stream(pinyin_meta_columns).map(c -> c + " = ?").collect(Collectors.joining(", "))));
    }

    private void endBatch() throws Exception {
        this.existWordMap.clear();
        this.existPinyinMap.clear();

        executeStatement(this.wordInsertStatement);
        executeStatement(this.wordUpdateStatement);
        executeStatement(this.pinyinInsertStatement);
        executeStatement(this.pinyinUpdateStatement);

        this.wordInsertStatement = null;
        this.wordUpdateStatement = null;
        this.pinyinInsertStatement = null;
        this.pinyinUpdateStatement = null;
    }

    /** 新增或更新{@link PinyinDict.Word 字}，返回带 id 的字信息 */
    public void saveWord(PinyinDict.Word word) {
        PinyinDict.Word savedWord = this.existWordMap.get(word.getValue());

        if (savedWord != null) {
            savedWord.setStrokeCount(word.getStrokeCount());
            savedWord.setStrokeOrder(word.getStrokeOrder());
            savedWord.setSimpleWord(word.getSimpleWord());

            System.out.println("Update word: " + word.getValue());
            updateWord(savedWord);
        } else {
            System.out.println("Add word: " + word.getValue());
            insertWord(word);
        }
    }

    /** 新增或更新{@link PinyinTree.Pinyin 拼音}，返回带 id 的拼音信息 */
    public void savePinyin(PinyinTree.Pinyin pinyin) {
        PinyinTree.Pinyin savedPinyin = this.existPinyinMap.get(pinyin.getValue() + ":" + pinyin.getWord());

        if (savedPinyin != null) {
            savedPinyin.setChars(pinyin.getChars());
            savedPinyin.setWeight(pinyin.getWeight());

            System.out.println("Update pinyin: " + pinyin.getValue() + " + " + pinyin.getWord());
            updatePinyin(savedPinyin);
        } else {
            System.out.println("Add pinyin: " + pinyin.getValue() + " + " + pinyin.getWord());
            insertPinyin(pinyin);
        }
    }

    private void updateWord(PinyinDict.Word data) {
        Map<String, Object> dataMap = createDataMap(data);

        setStatementParameter(this.wordUpdateStatement, word_meta_columns.length + 1, data.getId());
        fillBatchStatement(this.wordUpdateStatement, word_meta_columns, dataMap);
    }

    private void insertWord(PinyinDict.Word data) {
        Map<String, Object> dataMap = createDataMap(data);
        fillBatchStatement(this.wordInsertStatement, word_meta_columns, dataMap);
    }

    private void updatePinyin(PinyinTree.Pinyin data) {
        Map<String, Object> dataMap = createDataMap(data);

        setStatementParameter(this.pinyinUpdateStatement, pinyin_meta_columns.length + 1, data.getId());
        fillBatchStatement(this.pinyinUpdateStatement, pinyin_meta_columns, dataMap);
    }

    private void insertPinyin(PinyinTree.Pinyin data) {
        Map<String, Object> dataMap = createDataMap(data);
        fillBatchStatement(this.pinyinInsertStatement, pinyin_meta_columns, dataMap);
    }

    private List<PinyinDict.Word> queryAllWords() {
        return doAllQuery("pinyin_word", new String[] {
                "id_", "value_", "stroke_count_", "stroke_order_", "simple_word_"
        }, (result) -> {
            PinyinDict.Word data = new PinyinDict.Word();
            data.setId(result.getInt(1));
            data.setValue(result.getString(2));
            data.setStrokeCount(result.getInt(3));
            data.setStrokeOrder(result.getString(4));
            data.setSimpleWord(result.getString(5));

            return data;
        });
    }

    private List<PinyinTree.Pinyin> queryAllPinyins() {
        return doAllQuery("pinyin_pinyin", new String[] {
                "id_", "value_", "chars_", "weight_", "word_"
        }, (result) -> {
            PinyinTree.Pinyin data = new PinyinTree.Pinyin();
            data.setId(result.getInt(1));
            data.setValue(result.getString(2));
            data.setChars(result.getString(3));
            data.setWeight(result.getFloat(4));
            data.setWord(result.getString(5));

            return data;
        });
    }

    private Map<String, Object> createDataMap(PinyinDict.Word data) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value_", data.getValue());
        map.put("stroke_count_", data.getStrokeCount());
        map.put("stroke_order_", data.getStrokeOrder());
        map.put("simple_word_id_", null);

        if (data.isTraditional()) {
            PinyinDict.Word simple = this.existWordMap.get(data.getSimpleWord());
            if (simple != null) {
                map.put("simple_word_id_", simple.getId());
            }
        }

        return map;
    }

    private Map<String, Object> createDataMap(PinyinTree.Pinyin data) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value_", data.getValue());
        map.put("chars_", data.getChars());
        map.put("weight_", data.getWeight());

        PinyinDict.Word word = this.existWordMap.get(data.getWord());
        map.put("word_id_", word.getId());

        return map;
    }

    private void prepareSQLite() throws Exception {
        // https://www.sqlitetutorial.net/sqlite-create-table/
        // https://www.sqlitetutorial.net/sqlite-primary-key/
        // https://www.sqlite.org/datatype3.html
        String[] clauses = new String[] {
                // 原始表
                "CREATE TABLE\n"
                + "    IF NOT EXISTS pinyin_word_meta (\n"
                + "        id_ INTEGER NOT NULL PRIMARY KEY,\n"
                + "        value_ TEXT NOT NULL,\n"
                + "        stroke_count_ INTEGER DEFAULT 0,\n"
                + "        stroke_order_ TEXT DEFAULT '',\n"
                + "        simple_word_id_ INTEGER DEFAULT NUll,\n"
                + "        UNIQUE (value_),\n"
                + "        FOREIGN KEY (simple_word_id_) REFERENCES pinyin_word_meta (id_) ON DELETE SET NULL\n"
                + "    );",
                "CREATE TABLE\n"
                + "    IF NOT EXISTS pinyin_pinyin_meta (\n"
                + "        id_ INTEGER NOT NULL PRIMARY KEY,\n"
                + "        value_ TEXT NOT NULL,\n"
                + "        chars_ TEXT NOT NULL,\n"
                + "        word_id_ INTEGER NOT NUll,\n"
                + "        weight_ REAL DEFAULT 0,\n"
                + "        UNIQUE (value_, word_id_),\n"
                + "        FOREIGN KEY (word_id_) REFERENCES pinyin_word_meta (id_) ON DELETE CASCADE\n"
                + "    );",
                // 视图
                "CREATE VIEW\n"
                + "    IF NOT EXISTS pinyin_word (\n"
                + "        id_,\n"
                + "        value_,\n"
                + "        stroke_count_,\n"
                + "        stroke_order_,\n"
                + "        simple_word_\n"
                + "    ) AS\n"
                + "SELECT\n"
                + "    w_.id_,\n"
                + "    w_.value_,\n"
                + "    w_.stroke_count_,\n"
                + "    w_.stroke_order_,\n"
                + "    sw_.value_\n"
                + "FROM\n"
                + "    pinyin_word_meta w_\n"
                + "    LEFT JOIN pinyin_word_meta sw_ ON sw_.id_ = w_.simple_word_id_;",
                "CREATE VIEW\n"
                + "    IF NOT EXISTS pinyin_pinyin (\n"
                + "        id_,\n"
                + "        value_,\n"
                + "        chars_,\n"
                + "        weight_,\n"
                + "        word_\n"
                + "    ) AS\n"
                + "SELECT\n"
                + "    py_.id_,\n"
                + "    py_.value_,\n"
                + "    py_.chars_,\n"
                + "    py_.weight_,\n"
                + "    w_.value_\n"
                + "FROM\n"
                + "    pinyin_pinyin_meta py_\n"
                + "    LEFT JOIN pinyin_word_meta w_ ON w_.id_ = py_.word_id_;",
                // 提升批量写入性能: https://avi.im/blag/2021/fast-sqlite-inserts/
                "PRAGMA journal_mode = OFF;",
                "PRAGMA synchronous = 0;",
                "PRAGMA cache_size = 1000000;",
                "PRAGMA locking_mode = EXCLUSIVE;",
                "PRAGMA temp_store = MEMORY;",
                };

        try (Statement statement = this.connection.createStatement();) {
            for (String clause : clauses) {
                statement.execute(clause);
            }
        }
    }

    private <T> List<T> doAllQuery(
            String table, String[] columns, ResultSetParser<T> parser
    ) {
        try (
                Statement statement = this.connection.createStatement();
                ResultSet resultSet = statement.executeQuery(String.format("select %s from %s",
                                                                           String.join(", ", columns),
                                                                           table));
        ) {
            List<T> list = new ArrayList<>(resultSet.getRow());
            while (resultSet.next()) {
                T data = parser.parse(resultSet);
                list.add(data);
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void fillBatchStatement(PreparedStatement statement, String[] columns, Map<String, Object> data) {
        for (int i = 0; i < columns.length; i++) {
            int index = i + 1;
            String column = columns[i];
            Object value = data.get(column);

            setStatementParameter(statement, index, value);
        }

        try {
            statement.addBatch();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setStatementParameter(PreparedStatement statement, int index, Object value) {
        try {
            if (value == null) {
                statement.setNull(index, Types.NULL);
            } else if (value instanceof Float) {
                statement.setFloat(index, (Float) value);
            } else if (value instanceof Integer) {
                statement.setInt(index, (Integer) value);
            } else if (value instanceof String) {
                statement.setString(index, value.toString());
            } else {
                throw new IllegalStateException("Unknown data type: " + value.getClass().getName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void executeStatement(Statement statement) throws Exception {
        if (statement != null) {
            statement.executeBatch();
            statement.close();
        }
    }

    private interface ResultSetParser<T> {

        T parse(ResultSet resultSet) throws Exception;
    }

    public interface BatchCaller {

        void call() throws Exception;
    }
}
