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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.reflect.TypeToken;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinCharLink;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinTree;
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
        PinyinWordDataset wordDataset = createWordDataset();
        //analyzeWordDataset(wordDataset);

        Map<String, List<String[]>> phraseMap = readPhraseFromPhrasePinyinData();

        PinyinDict dict = createPinyinDict(wordDataset);

        // 拼音字典写入 Android 资源
        String dictJson = GsonUtils.toJson(dict);
        File dictResFile = new File("./src/main/res/raw/pinyin_dict.json");
        write(dictResFile, dictJson);

        // 生成字母连接线
        File linksFile = new File("../../analyze/files/char-links.json");
        List<PinyinCharLink> links = dict.getTree().createPinyinCharLinks();
        String linksJson = GsonUtils.toJson(links, null, new String[] { "undirected" });
        write(linksFile, linksJson);

        // 生成拼音列表
        File pinyinFile = new File("../../analyze/files/pinyin.txt");
        List<String> pinYinList = dict.getTree().createPinyinCharsList();
        write(pinyinFile, String.join("\n", pinYinList));
    }

    private PinyinDict createPinyinDict(PinyinWordDataset wordDataset) throws IOException {
        File wordLevel1File = new File("../../data/hanzi-level-1.txt");
        File wordLevel2File = new File("../../data/hanzi-level-2.txt");
        File wordLevel3File = new File("../../data/hanzi-level-3.txt");
        File wordWeightFile = new File("../../data/hanzi-weight.txt");

        Map<String, Integer> wordLevel1Map = readWordLevel(wordLevel1File, 1);
        Map<String, Integer> wordLevel2Map = readWordLevel(wordLevel2File, 2);
        Map<String, Integer> wordLevel3Map = readWordLevel(wordLevel3File, 3);
        Map<String, Float> wordWeightMap = readWordWeight(wordWeightFile);

        Map<String, PinyinWord> pinyinWordMap = mergeWordDataset(wordDataset);
        analyzeMergedWords(pinyinWordMap);

        PinyinDict dict = new PinyinDict();
        pinyinWordMap.forEach((word, pinyinWord) -> {
            float weight = wordWeightMap.getOrDefault(word, 0f);
            pinyinWord.setWeight(weight);

            for (Map<?, ?> map : (new Map[] {
                    wordLevel1Map, wordLevel2Map, wordLevel3Map
            })) {
                if (map.containsKey(word)) {
                    pinyinWord.setLevel((Integer) map.get(word));
                    break;
                }
            }

            PinyinDict.Word dictWord = new PinyinDict.Word();
            dictWord.setStrokeCount(pinyinWord.getStroke());
            dictWord.setStrokeOrder(pinyinWord.getStrokeOrder());

            if (pinyinWord.isTraditional() && !pinyinWord.getVariants().isEmpty()) {
                dictWord.setSimple(pinyinWord.getVariants().iterator().next());
            }

            List<String> pinyins = new ArrayList<>(pinyinWord.getPinyins());
            for (int i = 0; i < pinyins.size(); i++) {
                String pinyin = pinyins.get(i);

                dict.add(word, pinyin, dictWord, i == 0 ? weight : 0);
            }
        });
        System.out.println("===========================================");

        return dict;
    }

    private void analyzeMergedWords(Map<String, PinyinWord> pinyinWordMap) {
        Set<String> notExistWords = pinyinWordMap.entrySet()
                                                 .stream()
                                                 .filter(entry -> entry.getValue().getStroke() == 0)
                                                 .map(Map.Entry::getKey)
                                                 .collect(Collectors.toSet());
        System.out.printf("合并后没有笔画数的汉字列表(%d)：%s\n",
                          notExistWords.size(),
                          String.join(", ", notExistWords));

        notExistWords = pinyinWordMap.entrySet()
                                     .stream()
                                     .filter(entry -> entry.getValue().getStrokeOrder() == null)
                                     .map(Map.Entry::getKey)
                                     .collect(Collectors.toSet());
        System.out.printf("合并后没有笔顺的汉字列表(%d)：%s\n", notExistWords.size(), String.join(", ", notExistWords));

        notExistWords = pinyinWordMap.entrySet()
                                     .stream()
                                     .filter(entry -> entry.getValue().getPinyins().isEmpty())
                                     .map(Map.Entry::getKey)
                                     .collect(Collectors.toSet());
        System.out.printf("合并后没有拼音的汉字列表(%d)：%s\n", notExistWords.size(), String.join(", ", notExistWords));

        notExistWords = pinyinWordMap.entrySet()
                                     .stream()
                                     .filter(entry -> entry.getValue().getRadicals().isEmpty())
                                     .map(Map.Entry::getKey)
                                     .collect(Collectors.toSet());
        System.out.printf("合并后没有部首的汉字列表(%d)：%s\n", notExistWords.size(), String.join(", ", notExistWords));

        notExistWords = pinyinWordMap.entrySet()
                                     .stream()
                                     .filter(entry -> entry.getValue().isTraditional())
                                     .map(Map.Entry::getKey)
                                     .collect(Collectors.toSet());
        System.out.printf("合并后的繁体字列表(%d)：%s\n", notExistWords.size(), String.join(", ", notExistWords));
    }

    private void analyzeWordDataset(PinyinWordDataset wordDataset) {
        Map<String, PinyinWord> wordInPinyinDataMap = wordDataset.fromPinyinData;
        Map<String, PinyinWord> wordInCnCharMap = wordDataset.fromCnChar;
        Map<String, PinyinWord> wordInZiDatasetMap = wordDataset.fromZiDataset;

        System.out.println("===========================================");
        System.out.printf("pinyin-data 包含汉字：%d\n", wordInPinyinDataMap.size());
        System.out.printf("cnchar 包含汉字：%d\n", wordInCnCharMap.size());
        System.out.printf("zi-dataset 包含汉字：%d\n", wordInZiDatasetMap.size());

        Set<String> notExistWords = new HashSet<>(wordInPinyinDataMap.keySet());
        notExistWords.removeAll(wordInCnCharMap.keySet());
        System.out.printf("\ncnchar 中不包含 pinyin-data 中的汉字列表(%d)：%s\n",
                          notExistWords.size(),
                          String.join(", ", notExistWords));
        notExistWords = new HashSet<>(wordInZiDatasetMap.keySet());
        notExistWords.removeAll(wordInCnCharMap.keySet());
        System.out.printf("cnchar 中不包含 zi-dataset 中的汉字列表(%d)：%s\n",
                          notExistWords.size(),
                          String.join(", ", notExistWords));

        notExistWords = new HashSet<>(wordInPinyinDataMap.keySet());
        notExistWords.removeAll(wordInZiDatasetMap.keySet());
        System.out.printf("\nzi-dataset 中不包含 pinyin-data 中的汉字列表(%d)：%s\n",
                          notExistWords.size(),
                          String.join(", ", notExistWords));
        notExistWords = new HashSet<>(wordInCnCharMap.keySet());
        notExistWords.removeAll(wordInZiDatasetMap.keySet());
        System.out.printf("zi-dataset 中不包含 cnchar 中的汉字列表(%d)：%s\n",
                          notExistWords.size(),
                          String.join(", ", notExistWords));

        notExistWords = new HashSet<>(wordInCnCharMap.keySet());
        notExistWords.removeAll(wordInPinyinDataMap.keySet());
        System.out.printf("\npinyin-data 中不包含 cnchar 中的汉字列表(%d)：%s\n",
                          notExistWords.size(),
                          String.join(", ", notExistWords));
        notExistWords = new HashSet<>(wordInZiDatasetMap.keySet());
        notExistWords.removeAll(wordInPinyinDataMap.keySet());
        System.out.printf("pinyin-data 中不包含 zi-dataset 中的汉字列表(%d)：%s\n",
                          notExistWords.size(),
                          String.join(", ", notExistWords));

        notExistWords = wordInCnCharMap.entrySet()
                                       .stream()
                                       .filter(entry -> entry.getValue().getStrokeOrder() == null)
                                       .map(Map.Entry::getKey)
                                       .collect(Collectors.toSet());
        System.out.printf("\ncnchar 中没有笔顺的汉字列表(%d)：%s\n",
                          notExistWords.size(),
                          String.join(", ", notExistWords));
        notExistWords = wordInCnCharMap.entrySet()
                                       .stream()
                                       .filter(entry -> entry.getValue().getStroke() == 0)
                                       .map(Map.Entry::getKey)
                                       .collect(Collectors.toSet());
        System.out.printf("cnchar 中没有笔画数的汉字列表(%d)：%s\n",
                          notExistWords.size(),
                          String.join(", ", notExistWords));
        notExistWords = wordInCnCharMap.entrySet()
                                       .stream()
                                       .filter(entry -> entry.getValue().getRadicals().isEmpty())
                                       .map(Map.Entry::getKey)
                                       .collect(Collectors.toSet());
        System.out.printf("cnchar 中没有部首的汉字列表(%d)：%s\n",
                          notExistWords.size(),
                          String.join(", ", notExistWords));

        notExistWords = wordInZiDatasetMap.entrySet()
                                          .stream()
                                          .filter(entry -> entry.getValue().getStrokeOrder() == null)
                                          .map(Map.Entry::getKey)
                                          .collect(Collectors.toSet());
        System.out.printf("\nzi-dataset 中没有笔顺的汉字列表(%d)：%s\n",
                          notExistWords.size(),
                          String.join(", ", notExistWords));
    }

    private Map<String, PinyinWord> mergeWordDataset(PinyinWordDataset wordDataset) {
        Map<String, PinyinWord> wordInPinyinDataMap = wordDataset.fromPinyinData;
        Map<String, PinyinWord> wordInCnCharMap = wordDataset.fromCnChar;
        Map<String, PinyinWord> wordInZiDatasetMap = wordDataset.fromZiDataset;

        Map<String, PinyinWord> resultMap = new HashMap<>(wordInPinyinDataMap);
        wordInCnCharMap.forEach((word, wordInCnChar) -> {
            if (wordInPinyinDataMap.containsKey(word)) {
                return;
            }

            // cnchar 的繁体拼音设置为其对应简体的拼音
            if (wordInCnChar.getPinyins().isEmpty() && !wordInCnChar.getVariants().isEmpty()) {
                for (String variant : wordInCnChar.getVariants()) {
                    PinyinWord pw = wordInPinyinDataMap.get(variant);
                    if (pw == null) {
                        pw = wordInCnCharMap.get(variant);
                    }

                    wordInCnChar.getPinyins().addAll(pw.getPinyins());
                }
            }

            resultMap.put(word, wordInCnChar);
        });

        resultMap.forEach((word, mergedWord) -> {
            PinyinWord wordInZiDataset = wordInZiDatasetMap.get(word);

            if (wordInZiDataset == null) {
                System.out.printf("合并后在 zi-dataset 中不存在的字：%s\n", word);
            } else {
                // 从 zi-dataset 中补齐缺失信息
                if (mergedWord.getStroke() == 0) {
                    mergedWord.setStroke(wordInZiDataset.getStroke());
                }
                if (mergedWord.getRadicals().isEmpty()) {
                    mergedWord.getRadicals().addAll(wordInZiDataset.getRadicals());
                }
                if (mergedWord.getPinyins().isEmpty()) {
                    mergedWord.getPinyins().addAll(wordInZiDataset.getPinyins());
                }
                if (mergedWord.getVariants().isEmpty()) {
                    mergedWord.getVariants().addAll(wordInZiDataset.getVariants());
                }

                System.out.printf("从 zi-dataset 中合并的字：%s: %s;%s;%s;%d;%s;%s -> %s;%s;%s;%d;%s\n",
                                  word,
                                  String.join(", ", mergedWord.getPinyins()),
                                  String.join(", ", mergedWord.getRadicals()),
                                  String.join(", ", mergedWord.getVariants()),
                                  mergedWord.getStroke(),
                                  mergedWord.getStrokeOrder(),
                                  mergedWord.isTraditional(),
                                  String.join(", ", wordInZiDataset.getPinyins()),
                                  String.join(", ", wordInZiDataset.getRadicals()),
                                  String.join(", ", wordInZiDataset.getVariants()),
                                  wordInZiDataset.getStroke(),
                                  wordInZiDataset.getStrokeOrder());
            }
        });

        return resultMap;
    }

    private PinyinWordDataset createWordDataset() throws IOException {
        Map<String, PinyinWord> wordInPinyinDataMap = readWordFromPinyinData();
        Map<String, PinyinWord> wordInCnCharMap = readWordFromCnChar();
        Map<String, PinyinWord> wordInZiDatasetMap = readWordFromZiDataset();

        return new PinyinWordDataset(wordInPinyinDataMap, wordInCnCharMap, wordInZiDatasetMap);
    }

    private Map<String, PinyinWord> readWordFromPinyinData() throws IOException {
        File traditionalAndSimpleFile = new File("../../data/hanzi-traditional-to-simple.txt");
        Map<String, String> traditionalAndSimpleMap = readWordTraditionalToSimple(traditionalAndSimpleFile);
        Map<String, List<String>> simpleAndTraditionalMap = new HashMap<>();
        traditionalAndSimpleMap.forEach((trad, simple) -> {
            simpleAndTraditionalMap.computeIfAbsent(simple, (k) -> new ArrayList<>()).add(trad);
        });

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
                new PinyinWord("〇").addPinyin("líng")
        };

        Map<String, PinyinWord> resultMap = new HashMap<>();
        for (PinyinDataSource pinyinDataSource : pinyinDataSources) {
            List<String> lines = read(pinyinDataSource.file);
            for (String line : lines) {
                PinyinWord pw = parseWordInPinyinData(line, pinyinDataSource.ignoredCodeFrom);
                if (pw == null) {
                    continue;
                }

                boolean traditional = traditionalAndSimpleMap.containsKey(pw.getWord());
                pw.setTraditional(traditional);

                List<String> variants = simpleAndTraditionalMap.get(pw.getWord());
                if (variants != null) {
                    pw.getVariants().addAll(variants);
                }

                String variant = traditionalAndSimpleMap.get(pw.getWord());
                if (variant != null) {
                    pw.addVariant(variant);
                }

                resultMap.put(pw.getWord(), pw);
            }
        }
        for (PinyinWord pw : extraWords) {
            resultMap.put(pw.getWord(), pw);
        }

        return resultMap;
    }

    private Map<String, List<String[]>> readPhraseFromPhrasePinyinData() throws IOException {
        File file = new File("../../data/phrase-pinyin-data/large_pinyin.txt");
        List<String> lines = read(file);

        Map<String, List<String[]>> resultMap = new HashMap<>();
        for (String line : lines) {
            line = line.replaceAll("\\s*#.*$", "");
            if (line.isEmpty()) {
                continue;
            }

            String phrase = line.replaceAll("^([^:]+):.+", "$1");
            String[] pinyins = line.replaceAll("^.+:\\s+(.+)$", "$1").split("\\s+");

            resultMap.computeIfAbsent(phrase, (k) -> new ArrayList<>()).add(pinyins);
        }

        return resultMap;
    }

    private Map<String, PinyinWord> readWordFromZiDataset() throws IOException {
        // https://github.com/crazydan-studio/zi-dataset
        File file = new File("../../data/zi-dataset/zi-dataset.tsv");
        List<String> lines = read(file);
        // 第一行为标题行
        lines.remove(0);

        Map<String, PinyinWord> resultMap = new HashMap<>(lines.size());
        for (String line : lines) {
            String[] splits = line.split("\t");
            String word = splits[0];
            String[] pinyins = splits[3].split("\\s*,\\s*");
            String stroke = splits[1].replace("画", "");
            String[] radicals = splits[6].replaceAll("\\s*\\(.+$", "").split("/");
            String variant = splits.length >= 11 ? splits[10] != null ? splits[10].trim() : "" : "";
            String strokeOrder = splits.length >= 14 ? splits[13] != null ? splits[13].trim() : "" : "";

            int strokeCount;
            if (stroke.isEmpty()) {
                stroke = splits[2].replaceAll("^.+(\\d+)$", "$1");
                strokeCount = Integer.parseInt(stroke) + 1;
            } else {
                strokeCount = Integer.parseInt(stroke);
            }

            PinyinWord pw = new PinyinWord(word);
            pw.addPinyin(pinyins);
            pw.setStroke(strokeCount);
            pw.addRadical(radicals);

            if (!strokeOrder.isEmpty()) {
                pw.setStrokeOrder(strokeOrder);
            }

            if (!variant.isEmpty()) {
                pw.addVariant(variant);
            }

            resultMap.put(pw.getWord(), pw);
        }
        return resultMap;
    }

    private Map<String, PinyinWord> readWordFromCnChar() throws IOException {
        File simpleWordStrokeFile = new File("../../data/cnchar/src/cnchar/main/dict/stroke-count-jian.json");
        File simpleWordStrokeOrderFile = new File(
                "../../data/cnchar/src/cnchar/plugin/order/dict/stroke-order-jian.json");
        File traditionalWordStrokeFile
                = new File("../../data/cnchar/src/cnchar/plugin/trad/dict/stroke-count-trad.json");
        File traditionalWordStrokeOrderFile = new File(
                "../../data/cnchar/src/cnchar/plugin/trad/dict/stroke-order-trad.json");
        File traditionalAndSimpleFile = new File("../../data/cnchar/src/cnchar/plugin/trad/dict/trad-simple.json");
        File wordRadicalFile = new File("../../data/cnchar/src/cnchar/plugin/radical/dict/radicals.json");
        File wordPinyinsFile = new File("../../data/cnchar/src/cnchar/main/dict/spell-dict-jian.json");

        Map<String, Integer> simpleWordStrokeMap = readWordStrokesFromCnChar(simpleWordStrokeFile);
        Map<String, Integer> traditionalWordStrokeMap = readWordStrokesFromCnChar(traditionalWordStrokeFile);
        Map<String, String> traditionalAndSimpleMap = readTraditionalAndSimpleFromCnChar(traditionalAndSimpleFile);
        Map<String, String> simpleWordStrokeOrderMap = readWordStrokeOrderFromCnChar(simpleWordStrokeOrderFile);
        Map<String, String> traditionalWordStrokeOrderMap
                = readWordStrokeOrderFromCnChar(traditionalWordStrokeOrderFile);
        Map<String, String[]> wordRadicalMap = readWordRadicalFromCnChar(wordRadicalFile);
        Map<String, List<String>> wordPinyinsMap = readWordPinyinFromCnChar(wordPinyinsFile);

        Map<String, List<String>> simpleAndTraditionalMap = new HashMap<>();
        traditionalAndSimpleMap.forEach((trad, simple) -> {
            simpleAndTraditionalMap.computeIfAbsent(simple, (k) -> new ArrayList<>()).add(trad);
        });

        Map<String, PinyinWord> resultMap = new HashMap<>();
        simpleWordStrokeMap.forEach((word, stroke) -> {
            PinyinWord pw = new PinyinWord(word);
            pw.setStroke(stroke);
            pw.setTraditional(false);

            List<String> pinyins = wordPinyinsMap.get(pw.getWord());
            if (pinyins != null) {
                pw.getPinyins().addAll(pinyins);
            }

            List<String> variants = simpleAndTraditionalMap.get(pw.getWord());
            if (variants != null) {
                pw.getVariants().addAll(variants);
            }

            String strokeOrder = simpleWordStrokeOrderMap.get(pw.getWord());
            if (strokeOrder != null) {
                pw.setStrokeOrder(strokeOrder);
            }

            String[] radical = wordRadicalMap.get(pw.getWord());
            if (radical != null) {
                pw.addRadical(radical[0]);
                pw.setStruct(radical[1]);
            }

            resultMap.put(pw.getWord(), pw);
        });
        traditionalWordStrokeMap.forEach((word, stroke) -> {
            Integer simpleStroke = simpleWordStrokeMap.get(word);
            if (simpleStroke != null) {
                if (!Objects.equals(simpleStroke, stroke)) {
                    System.out.printf("在 cnchar 中繁简字笔画不一致：%s (S%d != T%d)\n", word, simpleStroke, stroke);
                }

                String simpleStrokeOrder = simpleWordStrokeOrderMap.get(word);
                String traditionalStrokeOrder = traditionalWordStrokeOrderMap.get(word);
                if (!Objects.equals(simpleStrokeOrder, traditionalStrokeOrder)) {
                    System.out.printf("在 cnchar 中繁简字笔顺不一致：%s (S:%s != T:%s)\n",
                                      word,
                                      simpleStrokeOrder,
                                      traditionalStrokeOrder);
                }
                return;
            }

            PinyinWord pw = new PinyinWord(word);
            pw.setStroke(stroke);
            pw.setTraditional(true);

            String variant = traditionalAndSimpleMap.get(pw.getWord());
            if (variant != null) {
                pw.addVariant(variant);
            }

            String strokeOrder = traditionalWordStrokeOrderMap.get(pw.getWord());
            if (strokeOrder != null) {
                pw.setStrokeOrder(strokeOrder);
            }

            String[] radical = wordRadicalMap.get(pw.getWord());
            if (radical != null) {
                pw.addRadical(radical[0]);
                pw.setStruct(radical[1]);
            }

            resultMap.put(pw.getWord(), pw);
        });

        return resultMap;
    }

    private Map<String, Integer> readWordStrokesFromCnChar(File file) throws IOException {
        // https://github.com/theajack/cnchar/blob/master/src/cnchar/plugin/trad/dict/stroke-count-trad.json
        List<String> lines = read(file);
        Map<String, String> dataMap = GsonUtils.fromJson(String.join("", lines),
                                                         new TypeToken<HashMap<String, String>>() {});

        Map<String, Integer> resultMap = new HashMap<>();
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            int strokes = Integer.parseInt(entry.getKey());
            String words = entry.getValue();

            for (int i = 0; i < words.length(); i++) {
                String word = words.charAt(i) + "";

                resultMap.put(word, strokes);
            }
        }

        return resultMap;
    }

    /** 繁体与简体的对应关系 */
    private Map<String, String> readTraditionalAndSimpleFromCnChar(File file) throws IOException {
        // https://github.com/theajack/cnchar/blob/master/src/cnchar/plugin/trad/dict/trad-simple.json
        List<String> lines = read(file);
        Map<String, String> dataMap = GsonUtils.fromJson(String.join("", lines),
                                                         new TypeToken<HashMap<String, String>>() {});

        Map<String, String> resultMap = new HashMap<>();
        String trads = dataMap.get("trad");
        String simples = dataMap.get("simple");
        if (trads.length() != simples.length()) {
            System.out.printf("在 cnchar 中繁简字隐射数量不一致：S%d != T%d\n", simples.length(), trads.length());
        }

        for (int i = 0; i < trads.length(); i++) {
            String trad = trads.charAt(i) + "";
            String simple = simples.charAt(i) + "";

            resultMap.put(trad, simple);
        }

        return resultMap;
    }

    /** 笔顺 */
    private Map<String, String> readWordStrokeOrderFromCnChar(File file) throws IOException {
        // https://github.com/theajack/cnchar/blob/master/src/cnchar/plugin/order/dict/stroke-order-jian.json
        List<String> lines = read(file);
        Map<String, String> dataMap = GsonUtils.fromJson(String.join("", lines),
                                                         new TypeToken<HashMap<String, String>>() {});
        return dataMap;
    }

    /** 部首及结构 */
    private Map<String, String[]> readWordRadicalFromCnChar(File file) throws IOException {
        // https://github.com/theajack/cnchar/blob/master/src/cnchar/plugin/radical/dict/radicals.json
        List<String> lines = read(file);
        Map<String, String> dataMap = GsonUtils.fromJson(String.join("", lines),
                                                         new TypeToken<HashMap<String, String>>() {});

        Map<String, String[]> resultMap = new HashMap<>();
        dataMap.forEach((radical, words) -> {
            if (radical.equals("*")) {
                return;
            }

            words = words.replaceAll("^\\d+:", "");
            for (int i = 0; i < words.length(); i += 2) {
                String word = words.charAt(i) + "";
                String struct = words.charAt(i + 1) + "";

                resultMap.put(word, new String[] { radical, struct });
            }
        });

        return resultMap;
    }

    private Map<String, List<String>> readWordPinyinFromCnChar(File file) throws IOException {
        // https://github.com/theajack/cnchar/blob/master/src/cnchar/main/dict/spell-dict-jian.json
        List<String> lines = read(file);
        Map<String, String> dataMap = GsonUtils.fromJson(String.join("", lines),
                                                         new TypeToken<HashMap<String, String>>() {});

        Map<String, List<String>> pinyinToneMap = new HashMap<>();
        for (Map.Entry<String, String> entry : PinyinTree.pinyinCharReplacements.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            List<String> list = pinyinToneMap.computeIfAbsent(value, (k) -> new ArrayList<>());
            if (list.isEmpty()) {
                list.add(value);
            }
            list.add(key);
        }

        Map<String, List<String>> resultMap = new HashMap<>();
        dataMap.forEach((spell, words) -> {
            int toneIndex = Integer.parseInt(words.replaceAll("^(\\d+):.+", "$1"));
            words = words.replaceAll("^\\d+:", "");

            for (int i = 0; i < words.length(); i += 2) {
                String word = words.charAt(i) + "";
                int tone = Integer.parseInt(words.charAt(i + 1) + "");
                // Note: 仅音调不同的多音字会在同一键内出现多次并在原音调上加 5
                tone = tone > 4 ? tone - 5 : tone;

                StringBuilder sb = new StringBuilder(spell);
                if (tone > 0) {
                    String toneChar = spell.charAt(toneIndex) + "";
                    // n 没有一声，故，前移音调
                    if (toneChar.equals("n")) {
                        tone -= 1;
                    }
                    toneChar = pinyinToneMap.get(toneChar).get(tone);

                    sb.replace(toneIndex, toneIndex + 1, toneChar);
                }

                resultMap.computeIfAbsent(word, (k) -> new ArrayList<>()).add(sb.toString());
            }
        });

        return resultMap;
    }

    private PinyinWord parseWordInPinyinData(String line, String ignoredCodeFrom) {
        if (line.startsWith("#")) {
            return null;
        }

        int codeValue = Integer.parseInt(line.replaceAll("^U\\+([^ ]+):.+", "$1"), 16);
        int ignoredCodeFromValue = Integer.parseInt(ignoredCodeFrom.replaceAll("^U\\+([^ ]+)", "$1"), 16);
        if (codeValue >= ignoredCodeFromValue) {
            return null;
        }

        line = line.replaceAll("^.+:\\s+", "");

        String word = line.replaceAll("^[^ ]+\\s+#\\s+([^ ]).*", "$1");
        String[] pinyins = line.replaceAll("^([^ ]+)\\s+.+", "$1").split("\\s*,\\s*");

        PinyinWord pinyinWord = new PinyinWord(word);
        for (String pinyin : pinyins) {
            pinyinWord.addPinyin(pinyin);
        }

        return pinyinWord;
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

    private static class PinyinWordDataset {
        private final Map<String, PinyinWord> fromPinyinData;
        private final Map<String, PinyinWord> fromCnChar;
        private final Map<String, PinyinWord> fromZiDataset;

        public PinyinWordDataset(
                Map<String, PinyinWord> fromPinyinData, Map<String, PinyinWord> fromCnChar,
                Map<String, PinyinWord> fromZiDataset
        ) {
            this.fromPinyinData = fromPinyinData;
            this.fromCnChar = fromCnChar;
            this.fromZiDataset = fromZiDataset;
        }
    }
}