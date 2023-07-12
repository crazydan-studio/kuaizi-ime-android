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

package org.crazydan.studio.app.ime.kuaizi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinCharLink;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinCharTree;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinTree;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.utils.GsonUtils;
import org.junit.Test;

/**
 * 生成拼音基础数据的单元测试
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-10
 */
public class PinyinDataTest {

    @Test
    public void test_generate_pinyin_data() throws Exception {
        // 数据来源: https://github.com/mozillazg/pinyin-data
        PinyinDataSource[] pinyinDataSources = new PinyinDataSource[] {
                // Note: 无繁体字
                new PinyinDataSource(new File("../../data/pinyin-data/kTGHZ2013.txt"), "U+20164"),
                // Note: 来源于《現代漢語頻率詞典》的拼音数据。一 仅有一个读音，不含 邓，含繁体字
                new PinyinDataSource(new File("../../data/pinyin-data/kHanyuPinlu.txt"), "U+FEEEE"),
                // Note: 来源于《漢語大字典》的拼音数据。一 仅有一个读音，邓 有多个读音，含繁体字
                //new PinyinDataSource(new File("../../data/pinyin-data/kHanyuPinyin.txt"),"U+20000"),
                // Note: 一 有多个读音，含繁体字
                //new PinyinDataSource(new File("../../data/pinyin-data/kXHC1983.txt"),"U+20201"),
                // Note: 没有多音字，仅标注常用读音，含繁体字
                //new PinyinDataSource(new File("../../data/pinyin-data/kMandarin.txt"),"U+20000"),
                // Note: 以上字典的合并数据
                //new PinyinDataSource(new File("../../data/pinyin-data/pinyin.txt"),"U+E815"),
        };
        // 补充字
        PinyinWord[] extraWords = new PinyinWord[] {
                // https://github.com/mozillazg/pinyin-data/blob/master/nonCJKUI.txt
                new PinyinWord("〇", "líng")
        };

        PinyinTree tree = new PinyinTree();
        for (PinyinDataSource pinyinDataSource : pinyinDataSources) {
            readToPinyinTree(tree, pinyinDataSource.file, pinyinDataSource.ignoredCodeFrom);
        }
        for (PinyinWord extraWord : extraWords) {
            tree.add(extraWord);
        }

        // 生成字母连接线
        boolean undirected = false;
        File linksFile = new File("../../analyze/files/char-links.json");
        List<PinyinCharLink> links = tree.charLinks(undirected);
        String linksJson = GsonUtils.toJson(links, null, new String[] { "undirected" });
        write(linksFile, linksJson);

        // 生成拼音字母组合树
        File treeFile = new File("../../analyze/files/char-tree.json");
        PinyinCharTree charTree = tree.charTree();
        String charJson = GsonUtils.toJson(charTree);
        write(treeFile, charJson);
        // 拼音字母组合树写入 Android 资源
        File treeResFile = new File("./src/main/res/raw/pinyin_char_tree.json");
        write(treeResFile, charJson);

        // 生成拼音列表
        File pinyinFile = new File("../../analyze/files/pinyin.txt");
        List<String> pinYinList = tree.getPinyinList();
        write(pinyinFile, String.join("\n", pinYinList));
    }

    private void readToPinyinTree(PinyinTree tree, File file, String ignoredCodeFrom) throws IOException {
        File wordLevel1File = new File("../../data/hanzi-level-1.txt");
        File wordLevel2File = new File("../../data/hanzi-level-2.txt");
        File wordLevel3File = new File("../../data/hanzi-level-3.txt");
        File wordWeightFile = new File("../../data/hanzi-weight.txt");
        File wordSTFile = new File("../../data/hanzi-traditional-to-simple.txt");
        File wordStrokeFile = new File("../../data/zi-dataset/zi-dataset.tsv");

        Map<String, Integer> wordLevel1Map = readWordLevel(wordLevel1File, 1);
        Map<String, Integer> wordLevel2Map = readWordLevel(wordLevel2File, 2);
        Map<String, Integer> wordLevel3Map = readWordLevel(wordLevel3File, 3);
        Map<String, Float> wordWeightMap = readWordWeight(wordWeightFile);
        Map<String, String> wordSTMap = readWordTraditionalToSimple(wordSTFile);
        Map<String, PinyinWord> wordStrokeMap = readWordStroke(wordStrokeFile);

        List<String> lines = read(file);

        for (String line : lines) {
            List<PinyinWord> pinyinWords = parse(line, ignoredCodeFrom);
            // 打印多音字
            if (pinyinWords.size() > 1) {
                System.out.println(line);
            }

            pinyinWords.forEach(word -> {
                String w = word.getWord();

                word.setTraditional(wordSTMap.containsKey(w));
                word.setWeight(wordWeightMap.getOrDefault(w, 0f));

                if (wordStrokeMap.containsKey(w)) {
                    PinyinWord pw = wordStrokeMap.get(w);
                    word.setStrokes(pw.getStrokes());
                } else {
                    word.setStrokes(0);
                }

                for (Map<?, ?> map : (new Map[] {
                        wordLevel1Map, wordLevel2Map, wordLevel3Map
                })) {
                    if (map.containsKey(w)) {
                        word.setLevel((Integer) map.get(w));
                        break;
                    }
                }

                tree.add(word);
            });
        }
    }

    private List<PinyinWord> parse(String line, String ignoredCodeFrom) {
        if (line.startsWith("#")) {
            return new ArrayList<>();
        }

        int codeValue = Integer.parseInt(line.replaceAll("^U\\+([^ ]+):.+", "$1"), 16);
        int ignoredCodeFromValue = Integer.parseInt(ignoredCodeFrom.replaceAll("^U\\+([^ ]+)", "$1"), 16);
        if (codeValue >= ignoredCodeFromValue) {
            return new ArrayList<>();
        }

        line = line.replaceAll("^.+:\\s+", "");

        List<PinyinWord> pinyinWords = new ArrayList<>();
        String word = line.replaceAll("^[^ ]+\\s+#\\s+([^ ]).*", "$1");
        String[] pinyins = line.replaceAll("^([^ ]+)\\s+.+", "$1").split("\\s*,\\s*");
        for (String pinyin : pinyins) {
            PinyinWord pinyinWord = new PinyinWord(word, pinyin);
            pinyinWords.add(pinyinWord);
        }

        return pinyinWords;
    }

    private Map<String, Integer> readWordLevel(File file, int level) throws IOException {
        List<String> lines = read(file);

        Map<String, Integer> map = new HashMap<>(lines.size());
        for (String line : lines) {
            if (!line.startsWith("#")) {
                map.put(line, level);
            }
        }
        return map;
    }

    private Map<String, Float> readWordWeight(File file) throws IOException {
        List<String> lines = read(file);

        Map<String, Float> map = new HashMap<>(lines.size());
        for (String line : lines) {
            if (!line.startsWith("#")) {
                String[] segments = line.split(",");
                String word = segments[0];
                float weight = Float.parseFloat(segments[1]);

                map.put(word, weight);
            }
        }
        return map;
    }

    private Map<String, String> readWordTraditionalToSimple(File file) throws IOException {
        List<String> lines = read(file);

        Map<String, String> map = new HashMap<>(lines.size());
        for (String line : lines) {
            if (!line.startsWith("#")) {
                String[] segments = line.split(",");
                String t = segments[0];
                String s = segments[1];

                map.put(t, s);
            }
        }
        return map;
    }

    private Map<String, PinyinWord> readWordStroke(File file) throws IOException {
        List<String> lines = read(file);
        // 第一行为标题行
        lines.remove(0);

        Map<String, PinyinWord> map = new HashMap<>(lines.size());
        for (String line : lines) {
            String[] splits = line.split("\t");
            String word = splits[0];
            String stroke = splits[1].replace("画", "");

            int strokeCount;
            if (stroke.isEmpty()) {
                stroke = splits[2].replaceAll("^.+(\\d+)$", "$1");
                strokeCount = Integer.parseInt(stroke) + 1;
            } else {
                strokeCount = Integer.parseInt(stroke);
            }

            PinyinWord pw = new PinyinWord(word, null);
            pw.setStrokes(strokeCount);

            map.put(word, pw);
        }
        return map;
    }

    private List<String> read(File file) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader bf = new BufferedReader(new FileReader(file));) {
            String line;
            while ((line = bf.readLine()) != null) {
                line = line.trim();
                lines.add(line);
            }
        }

        return lines;
    }

    private void write(File file, String content) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();
    }

    private static class PinyinDataSource {
        public final File file;
        public final String ignoredCodeFrom;

        private PinyinDataSource(File file, String ignoredCodeFrom) {
            this.file = file;
            this.ignoredCodeFrom = ignoredCodeFrom;
        }
    }
}