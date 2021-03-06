package com.star.easydoc.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.intellij.openapi.components.ServiceManager;
import com.star.easydoc.config.Consts;
import com.star.easydoc.config.EasyJavadocConfigComponent;
import com.star.easydoc.model.EasyJavadocConfiguration;
import com.star.easydoc.service.translator.Translator;
import com.star.easydoc.service.translator.impl.BaiduTranslator;
import com.star.easydoc.service.translator.impl.JinshanTranslator;
import com.star.easydoc.service.translator.impl.YoudaoCh2EnTranslator;
import com.star.easydoc.service.translator.impl.YoudaoEn2ChTranslator;
import com.star.easydoc.util.CollectionUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * @author wangchao
 * @date 2019/08/25
 */
public class TranslatorService {

    private EasyJavadocConfiguration config = ServiceManager.getService(EasyJavadocConfigComponent.class).getState();
    private Translator en2ChTranslator = new YoudaoCh2EnTranslator();
    private Map<String, Translator> translatorMap = ImmutableMap.<String, Translator>builder()
        .put("百度翻译", new BaiduTranslator())
        .put("金山翻译", new JinshanTranslator())
        .put("有道翻译", new YoudaoEn2ChTranslator())
        .build();

    public String translate(String source) {
        List<String> words = split(source);
        if (isCustomMode(words)) {
            // 有自定义单词，使用默认模式，单个单词翻译
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                String res = getFromCustom(word);
                if (StringUtils.isBlank(res)) {
                    res = getFromOthers(word);
                }
                if (StringUtils.isBlank(res)) {
                    res = word;
                }
                sb.append(res);
            }
            return sb.toString();
        } else {
            // 没有自定义单词，使用整句翻译，翻译更准确
            return getFromOthers(StringUtils.join(words, StringUtils.SPACE));
        }
    }

    public String translateCh2En(String source) {
        if (StringUtils.isBlank(source)) {
            return "";
        }
        String ch = en2ChTranslator.translate(source);
        String[] chs = StringUtils.split(ch);
        List<String> chList = chs == null ? Lists.newArrayList() : Lists.newArrayList(chs);
        chList = chList.stream().filter(c -> !Consts.STOP_WORDS.contains(c.toLowerCase())).collect(Collectors.toList());

        if (CollectionUtil.isEmpty(chList)) {
            return "";
        }
        if (chList.size() == 1) {
            return chList.get(0);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chList.size(); i++) {
            if (StringUtils.isBlank(chList.get(i))) {
                continue;
            }
            if (Consts.STOP_WORDS.contains(chList.get(i).toLowerCase())) {
                continue;
            }
            if (i == 0) {
                sb.append(chList.get(i).toLowerCase());
            } else {
                String lowCh = chList.get(i).toLowerCase();
                sb.append(StringUtils.substring(lowCh, 0, 1).toUpperCase()).append(StringUtils.substring(lowCh, 1));
            }
        }
        return sb.toString();
    }

    private List<String> split(String word) {
        word = word.replaceAll("(?<=[^A-Z])[A-Z][^A-Z]", "_$0");
        word = word.replaceAll("[A-Z]{2,}", "_$0");
        word = word.replaceAll("_+", "_");
        return Arrays.asList(word.split("_"));
    }

    /**
     * 是否自定义模式
     *
     * @param words 单词
     * @return boolean
     */
    private boolean isCustomMode(List<String> words) {
        return CollectionUtil.containsAny(config.getWordMap().keySet(), words);
    }

    private String getFromCustom(String word) {
        return config.getWordMap().get(word.toLowerCase());
    }

    private String getFromOthers(String word) {
        Translator translator = translatorMap.get(config.getTranslator());
        if (Objects.isNull(translator)) {
            return StringUtils.EMPTY;
        }
        return translator.translate(word);
    }

}
