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
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
    private final static String[] phrase_meta_columns = new String[] {
            "pre_pinyin_id_", "post_pinyin_id_", "weight_"
    };

    private final Map<String, PinyinDict.Word> existWordMap = new HashMap<>();
    private final Map<String, PinyinTree.Pinyin> existPinyinMap = new HashMap<>();
    private final Map<String, Chars> existCharsMap = new HashMap<>();
    private final Map<String, Phrase> existPhraseMap = new HashMap<>();

    private Connection connection;
    private BatchStatement wordBatchStatement;
    private BatchStatement pinyinBatchStatement;
    private BatchStatement charsBatchStatement;
    private BatchStatement phraseBatchStatement;

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
        queryAllWords().forEach(data -> {
            this.existWordMap.put(data.getValue(), data);
        });
        queryAllPinyins().forEach(data -> {
            this.existPinyinMap.put(data.getCode(), data);
        });
        queryAllChars().forEach(data -> {
            this.existCharsMap.put(data.getValue(), data);
        });
        queryAllPhrases().forEach(data -> {
            this.existPhraseMap.put(data.getCode(), data);
        });

        String[] columns = word_meta_columns;
        this.wordBatchStatement = new BatchStatement("pinyin_word_meta", columns);
        this.wordBatchStatement.prepare(this.connection);

        columns = pinyin_meta_columns;
        this.pinyinBatchStatement = new BatchStatement("pinyin_pinyin_meta", columns);
        this.pinyinBatchStatement.prepare(this.connection);

        columns = chars_meta_columns;
        this.charsBatchStatement = new BatchStatement("pinyin_chars_meta", columns);
        this.charsBatchStatement.prepare(this.connection);

        columns = phrase_meta_columns;
        this.phraseBatchStatement = new BatchStatement("pinyin_phrase_meta", columns);
        this.phraseBatchStatement.prepare(this.connection);
    }

    private void endBatch() throws Exception {
        this.existWordMap.clear();
        this.existPinyinMap.clear();
        this.existCharsMap.clear();
        this.existPhraseMap.clear();

        this.wordBatchStatement.submit();
        this.pinyinBatchStatement.submit();
        this.charsBatchStatement.submit();
        this.phraseBatchStatement.submit();
    }

    /** 新增或更新{@link PinyinDict.Word 字} */
    public void saveWord(PinyinDict.Word data) {
        PinyinDict.Word savedData = this.existWordMap.get(data.getValue());

        if (savedData != null) {
            boolean changed = false;
            changed = changed || doSet(savedData::getSimpleWord, savedData::setSimpleWord, data::getSimpleWord);

            if (changed) {
                this.wordBatchStatement.update(createDataMap(savedData), savedData::getId);
            }
        } else {
            this.wordBatchStatement.insert(createDataMap(data));
        }
    }

    /** 新增或更新{@link PinyinTree.Pinyin 拼音} */
    public void savePinyin(PinyinTree.Pinyin data) {
        PinyinTree.Pinyin savedData = this.existPinyinMap.get(data.getCode());

        if (savedData != null) {
            boolean changed = false;
            changed = changed || doSet(savedData::getChars, savedData::setChars, data::getChars);
            changed = changed || doSet(savedData::getWeight, savedData::setWeight, data::getWeight);

            if (changed) {
                this.pinyinBatchStatement.update(createDataMap(savedData), savedData::getId);
            }
        } else {
            this.pinyinBatchStatement.insert(createDataMap(data));
        }
    }

    /** 新增或更新{@link PinyinTree.Pinyin#chars 拼音字母} */
    public void savePinyinChars(String data) {
        Chars savedData = this.existCharsMap.get(data);

        if (savedData != null) {
            boolean changed = false;
            changed = changed || doSet(savedData::getValue, savedData::setValue, () -> data);

            if (changed) {
                this.charsBatchStatement.update(createDataMap(savedData), savedData::getId);
            }
        } else {
            savedData = new Chars();
            savedData.setValue(data);

            this.charsBatchStatement.insert(createDataMap(savedData));
        }
    }

    public void savePhrases(Set<PinyinTree.Phrase> phrases) {
        Map<String, Phrase> dataMap = new HashMap<>();
        phrases.forEach(phrase -> {
            PinyinTree.Pinyin pre = phrase.getPinyins().get(0);
            for (int i = 1; i < phrase.getPinyins().size(); i++) {
                PinyinTree.Pinyin post = phrase.getPinyins().get(i);

                if (!this.existPinyinMap.containsKey(pre.getCode())) {
                    System.out.printf("短语 %s 中不存在字 %s\n", phrase, pre.getCode());
                    continue;
                }
                if (!this.existPinyinMap.containsKey(post.getCode())) {
                    System.out.printf("短语 %s 中不存在字 %s\n", phrase, post.getCode());
                    continue;
                }

                int preId = this.existPinyinMap.get(pre.getCode()).getId();
                int postId = this.existPinyinMap.get(post.getCode()).getId();

                Phrase data = new Phrase();
                data.setPrePinyinId(preId);
                data.setPostPinyinId(postId);
                data.setWeight(phrase.getWeight());

                Phrase exist = dataMap.get(data.getCode());
                if (exist != null) {
                    exist.setWeight(exist.getWeight() + 1);
                } else {
                    dataMap.put(data.getCode(), data);
                }

                pre = post;
            }
        });

        dataMap.forEach((code, data) -> {
            Phrase savedData = this.existPhraseMap.get(data.getCode());

            if (savedData != null) {
                boolean changed = false;
                changed = changed || doSet(savedData::getWeight, savedData::setWeight, data::getWeight);

                if (changed) {
                    this.phraseBatchStatement.update(createDataMap(savedData), savedData::getId);
                }
            } else {
                this.phraseBatchStatement.insert(createDataMap(data));
            }
        });
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

    private List<Phrase> queryAllPhrases() {
        return doAllQuery("pinyin_phrase_meta", new String[] {
                "id_", "pre_pinyin_id_", "post_pinyin_id_", "weight_"
        }, (result) -> {
            Phrase data = new Phrase();
            data.setId(result.getInt(1));
            data.setPrePinyinId(result.getInt(2));
            data.setPostPinyinId(result.getInt(3));
            data.setWeight(result.getInt(4));

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

    private Map<String, Object> createDataMap(Phrase phrase) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pre_pinyin_id_", phrase.getPrePinyinId());
        map.put("post_pinyin_id_", phrase.getPostPinyinId());
        map.put("weight_", phrase.getWeight());

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
                "CREATE TABLE\n"
                + "    IF NOT EXISTS pinyin_phrase_meta (\n"
                + "        id_ INTEGER NOT NULL PRIMARY KEY,\n"
                + "        pre_pinyin_id_ INTEGER NOT NULL,\n"
                + "        post_pinyin_id_ INTEGER NOT NUll,\n"
                + "        weight_ INTEGER DEFAULT 0,\n"
                + "        UNIQUE (pre_pinyin_id_, post_pinyin_id_),\n"
                + "        FOREIGN KEY (pre_pinyin_id_) REFERENCES pinyin_pinyin_meta (id_),\n"
                + "        FOREIGN KEY (post_pinyin_id_) REFERENCES pinyin_pinyin_meta (id_)\n"
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
                + "    IF NOT EXISTS pinyin_pinyin (id_, value_, chars_, weight_, word_, simple_word_) AS\n"
                + "SELECT\n"
                + "    py_.id_,\n"
                + "    py_.value_,\n"
                + "    ch_.value_,\n"
                + "    py_.weight_,\n"
                + "    w_.value_,\n"
                + "    w_.simple_word_\n"
                + "FROM\n"
                + "    pinyin_pinyin_meta py_\n"
                + "    INNER JOIN pinyin_chars_meta ch_ ON ch_.id_ = py_.chars_id_\n"
                + "    LEFT JOIN pinyin_word w_ ON w_.id_ = py_.word_id_;",
                "CREATE VIEW\n"
                + "    IF NOT EXISTS pinyin_phrase (\n"
                + "        id_,\n"
                + "        weight_,\n"
                + "        pre_pinyin_,\n"
                + "        pre_chars_,\n"
                + "        pre_word_,\n"
                + "        post_pinyin_,\n"
                + "        post_chars_,\n"
                + "        post_word_\n"
                + "    ) AS\n"
                + "SELECT\n"
                + "    ph_.id_,\n"
                + "    ph_.weight_,\n"
                + "    pre_.value_,\n"
                + "    pre_.chars_,\n"
                + "    pre_.word_,\n"
                + "    post_.value_,\n"
                + "    post_.chars_,\n"
                + "    post_.word_\n"
                + "FROM\n"
                + "    pinyin_phrase_meta ph_\n"
                + "    INNER JOIN pinyin_pinyin pre_ ON pre_.id_ = ph_.pre_pinyin_id_\n"
                + "    INNER JOIN pinyin_pinyin post_ ON post_.id_ = ph_.post_pinyin_id_;",
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

    private <T> boolean doSet(Supplier<T> targetGetter, Consumer<T> targetSetter, Supplier<T> sourceGetter) {
        if (Objects.equals(targetGetter.get(), sourceGetter.get())) {
            return false;
        }

        targetSetter.accept(sourceGetter.get());
        return true;
    }

    private interface ResultSetParser<T> {

        T parse(ResultSet resultSet) throws Exception;
    }

    public interface BatchCaller {

        void call() throws Exception;
    }

    private static class BatchStatement {
        private final String table;
        private final String[] columns;

        private PreparedStatement insert;
        private PreparedStatement update;

        public BatchStatement(String table, String[] columns) {
            this.table = table;
            this.columns = columns;
        }

        public void prepare(Connection connection) throws Exception {
            this.insert = connection.prepareStatement(String.format("insert into %s (%s) values (%s)",
                                                                    this.table,
                                                                    String.join(", ", this.columns),
                                                                    Arrays.stream(this.columns)
                                                                          .map(c -> "?")
                                                                          .collect(Collectors.joining(", "))));
            this.update = connection.prepareStatement(String.format("update %s set %s where id_ = ?",
                                                                    this.table,
                                                                    Arrays.stream(this.columns)
                                                                          .map(c -> c + " = ?")
                                                                          .collect(Collectors.joining(", "))));
        }

        public void insert(Map<String, Object> data) {
            fillStatement(this.insert, this.columns, data);
        }

        public void update(Map<String, Object> data, Supplier<Integer> id) {
            setStatementParameter(this.update, this.columns.length + 1, id.get());
            fillStatement(this.update, this.columns, data);
        }

        public void submit() throws Exception {
            executeStatement(this.insert);
            executeStatement(this.update);

            this.insert = null;
            this.update = null;
        }

        private void executeStatement(Statement statement) throws Exception {
            if (statement != null) {
                statement.executeBatch();
                statement.close();
            }
        }

        private void fillStatement(PreparedStatement statement, String[] columns, Map<String, Object> data) {
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

    private static class Phrase {
        private int id;
        private int prePinyinId;
        private int postPinyinId;
        private int weight;

        public int getId() {
            return this.id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getCode() {
            return this.prePinyinId + ":" + this.postPinyinId;
        }

        public int getPrePinyinId() {
            return this.prePinyinId;
        }

        public void setPrePinyinId(int prePinyinId) {
            this.prePinyinId = prePinyinId;
        }

        public int getPostPinyinId() {
            return this.postPinyinId;
        }

        public void setPostPinyinId(int postPinyinId) {
            this.postPinyinId = postPinyinId;
        }

        public int getWeight() {
            return this.weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }
}
