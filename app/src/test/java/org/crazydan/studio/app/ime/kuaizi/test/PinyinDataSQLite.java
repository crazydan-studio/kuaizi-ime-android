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
            "value_", "simple_word_id_"
    };
    private final static String[] pinyin_meta_columns = new String[] {
            "value_", "chars_id_", "weight_", "word_id_"
    };
    private final static String[] chars_meta_columns = new String[] {
            "value_"
    };

    private final Map<String, PinyinDict.Word> existWordMap = new HashMap<>();
    private final Map<String, PinyinTree.Pinyin> existPinyinMap = new HashMap<>();
    private final Map<String, Chars> existCharsMap = new HashMap<>();

    private Connection connection;
    private PreparedStatement wordInsertStatement;
    private PreparedStatement wordUpdateStatement;
    private PreparedStatement pinyinInsertStatement;
    private PreparedStatement pinyinUpdateStatement;
    private PreparedStatement charsInsertStatement;
    private PreparedStatement charsUpdateStatement;

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
        queryAllWords().forEach(word -> {
            this.existWordMap.put(word.getValue(), word);
        });
        queryAllPinyins().forEach(pinyin -> {
            this.existPinyinMap.put(pinyin.getValue() + ":" + pinyin.getWord(), pinyin);
        });
        queryAllChars().forEach(chars -> {
            this.existCharsMap.put(chars.getValue(), chars);
        });

        String[] columns = word_meta_columns;
        this.wordInsertStatement = this.connection.prepareStatement(String.format(
                "insert into pinyin_word_meta (%s) values (%s)",
                String.join(", ", columns),
                Arrays.stream(columns).map(c -> "?").collect(Collectors.joining(", "))));
        this.wordUpdateStatement = this.connection.prepareStatement(String.format(
                "update pinyin_word_meta set %s where id_ = ?",
                Arrays.stream(columns).map(c -> c + " = ?").collect(Collectors.joining(", "))));

        columns = pinyin_meta_columns;
        this.pinyinInsertStatement = this.connection.prepareStatement(String.format(
                "insert into pinyin_pinyin_meta (%s) values (%s)",
                String.join(", ", columns),
                Arrays.stream(columns).map(c -> "?").collect(Collectors.joining(", "))));
        this.pinyinUpdateStatement = this.connection.prepareStatement(String.format(
                "update pinyin_pinyin_meta set %s where id_ = ?",
                Arrays.stream(columns).map(c -> c + " = ?").collect(Collectors.joining(", "))));

        columns = chars_meta_columns;
        this.charsInsertStatement = this.connection.prepareStatement(String.format(
                "insert into pinyin_chars_meta (%s) values (%s)",
                String.join(", ", columns),
                Arrays.stream(columns).map(c -> "?").collect(Collectors.joining(", "))));
        this.charsUpdateStatement = this.connection.prepareStatement(String.format(
                "update pinyin_chars_meta set %s where id_ = ?",
                Arrays.stream(columns).map(c -> c + " = ?").collect(Collectors.joining(", "))));
    }

    private void endBatch() throws Exception {
        this.existWordMap.clear();
        this.existPinyinMap.clear();

        executeStatement(this.wordInsertStatement);
        executeStatement(this.wordUpdateStatement);
        executeStatement(this.pinyinInsertStatement);
        executeStatement(this.pinyinUpdateStatement);
        executeStatement(this.charsInsertStatement);
        executeStatement(this.charsUpdateStatement);

        this.wordInsertStatement = null;
        this.wordUpdateStatement = null;
        this.pinyinInsertStatement = null;
        this.pinyinUpdateStatement = null;
        this.charsInsertStatement = null;
        this.charsUpdateStatement = null;
    }

    /** 新增或更新{@link PinyinDict.Word 字} */
    public void saveWord(PinyinDict.Word data) {
        PinyinDict.Word savedData = this.existWordMap.get(data.getValue());

        if (savedData != null) {
            savedData.setStrokeCount(data.getStrokeCount());
            savedData.setStrokeOrder(data.getStrokeOrder());
            savedData.setSimpleWord(data.getSimpleWord());

            updateWord(savedData);
        } else {
            insertWord(data);
        }
    }

    /** 新增或更新{@link PinyinTree.Pinyin 拼音} */
    public void savePinyin(PinyinTree.Pinyin data) {
        PinyinTree.Pinyin savedData = this.existPinyinMap.get(data.getValue() + ":" + data.getWord());

        if (savedData != null) {
            savedData.setChars(data.getChars());
            savedData.setWeight(data.getWeight());

            updatePinyin(savedData);
        } else {
            insertPinyin(data);
        }
    }

    /** 新增或更新{@link PinyinTree.Pinyin#chars 拼音字母} */
    public void saveChars(String data) {
        Chars savedData = this.existCharsMap.get(data);

        if (savedData != null) {
            savedData.setValue(data);

            updateChars(savedData);
        } else {
            savedData = new Chars();
            savedData.setValue(data);

            insertChars(savedData);
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

    private void updateChars(Chars data) {
        Map<String, Object> dataMap = createDataMap(data);

        setStatementParameter(this.charsUpdateStatement, chars_meta_columns.length + 1, data.getId());
        fillBatchStatement(this.charsUpdateStatement, chars_meta_columns, dataMap);
    }

    private void insertChars(Chars data) {
        Map<String, Object> dataMap = createDataMap(data);
        fillBatchStatement(this.charsInsertStatement, chars_meta_columns, dataMap);
    }

    private List<PinyinDict.Word> queryAllWords() {
        return doAllQuery("pinyin_word", new String[] {
                "id_", "value_", "simple_word_"
        }, (result) -> {
            PinyinDict.Word data = new PinyinDict.Word();
            data.setId(result.getInt(1));
            data.setValue(result.getString(2));
            data.setSimpleWord(result.getString(3));

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
            data.setWeight(result.getInt(4));
            data.setWord(result.getString(5));

            return data;
        });
    }

    private List<Chars> queryAllChars() {
        return doAllQuery("pinyin_chars_meta", new String[] {
                "id_", "value_"
        }, (result) -> {
            Chars data = new Chars();
            data.setId(result.getInt(1));
            data.setValue(result.getString(2));

            return data;
        });
    }

    private Map<String, Object> createDataMap(PinyinDict.Word data) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value_", data.getValue());
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
        map.put("weight_", data.getWeight());

        Chars chars = this.existCharsMap.get(data.getChars());
        map.put("chars_id_", chars.getId());

        PinyinDict.Word word = this.existWordMap.get(data.getWord());
        map.put("word_id_", word.getId());

        return map;
    }

    private Map<String, Object> createDataMap(Chars data) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value_", data.getValue());

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
                + "        simple_word_id_ INTEGER DEFAULT NUll,\n"
                + "        UNIQUE (value_),\n"
                + "        FOREIGN KEY (simple_word_id_) REFERENCES pinyin_word_meta (id_)\n"
                + "    );",
                "CREATE TABLE\n"
                + "    IF NOT EXISTS pinyin_chars_meta (\n"
                + "        id_ INTEGER NOT NULL PRIMARY KEY,\n"
                + "        value_ TEXT NOT NULL,\n"
                + "        UNIQUE (value_)\n"
                + "    );",
                "CREATE TABLE\n"
                + "    IF NOT EXISTS pinyin_pinyin_meta (\n"
                + "        id_ INTEGER NOT NULL PRIMARY KEY,\n"
                + "        value_ TEXT NOT NULL,\n"
                + "        chars_id_ INTEGER NOT NULL,\n"
                + "        word_id_ INTEGER NOT NUll,\n"
                + "        weight_ INTEGER DEFAULT 0,\n"
                + "        UNIQUE (value_, word_id_),\n"
                + "        FOREIGN KEY (word_id_) REFERENCES pinyin_word_meta (id_),\n"
                + "        FOREIGN KEY (chars_id_) REFERENCES pinyin_chars_meta (id_)\n"
                + "    );",
                // 视图
                "CREATE VIEW\n"
                + "    IF NOT EXISTS pinyin_word (id_, value_, simple_word_) AS\n"
                + "SELECT\n"
                + "    w_.id_,\n"
                + "    w_.value_,\n"
                + "    sw_.value_\n"
                + "FROM\n"
                + "    pinyin_word_meta w_\n"
                + "    LEFT JOIN pinyin_word_meta sw_ ON sw_.id_ = w_.simple_word_id_;",
                "CREATE VIEW\n"
                + "    IF NOT EXISTS pinyin_pinyin (id_, value_, chars_, weight_, word_) AS\n"
                + "SELECT\n"
                + "    py_.id_,\n"
                + "    py_.value_,\n"
                + "    ch_.value_,\n"
                + "    py_.weight_,\n"
                + "    w_.value_\n"
                + "FROM\n"
                + "    pinyin_pinyin_meta py_\n"
                + "    INNER JOIN pinyin_chars_meta ch_ ON ch_.id_ = py_.chars_id_\n"
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

    private static class Chars {
        private int id;
        private String value;

        public int getId() {
            return this.id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
