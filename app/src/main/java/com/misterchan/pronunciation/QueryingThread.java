package com.misterchan.pronunciation;

import android.database.CursorJoiner;
import android.graphics.Color;
import android.icu.util.Measure;
import android.media.MediaDrm;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryingThread extends Thread {

    private static final ViewGroup.LayoutParams LAYOUT_PARAMS = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    private static final Pattern PATTERN_CUHK_EXPL = Pattern.compile("(?<=<td><div nowrap>).*?(?=(</div>)?</td>)");
    private static final Pattern PATTERN_CUHK_PRON = Pattern.compile("(?<=\"sound\\.php\\?s=)[a-z1-6]+(?=\")");
    private static final Pattern PATTERN_SHYYP_CHAR = Pattern.compile("(?<=<table class=\" w-full\"><tbody><tr>).+?(?=</tr></tbody></table>)");
    private static final Pattern PATTERN_SHYYP_CHAR_PRON = Pattern.compile("(?<=<span class=\"PSX  text-xl pl-2 pr-1 py-2 PS_jyutping \">)[a-z]+?[1-6](?=</span>)");
    private static final Pattern PATTERN_SHYYP_EXPL = Pattern.compile("(?<=<ul class=\"my-2\"><li><span>).*?(?=</span><.*></li></ul>)");
    private static final Pattern PATTERN_SHYYP_PRON = Pattern.compile("(?<=<!-- --> <span class=\"PSX  text-xl pl-2 pr-1 py-2 PS_jyutping \">)[a-z]+?[1-6](?=</span>)");
    private static final String BIG5 = "BIG5";
    private static final String CUHK = "https://humanum.arts.cuhk.edu.hk/Lexis/lexi-can/search.php?q=";
    private static final String LOADING = "加載中…(%d%%)";
    private static final String SHYYP = "https://shyyp.net/search?q=";
    private static final String SHYYP_1 = "https://shyyp.net/w/";
    private static final String UTF_8 = "UTF-8";

    private final MainActivity mainActivity;
    private final String chars;
    private final String schema;
    private final String url;

    private static final Map<String, Integer> CHARS_LENGTH_PER_PAGE = new HashMap<String, Integer>() {
        {
            put(URLs.CUHK, 1);
            put(URLs.SHYYP, 129);
        }
    };

    public QueryingThread(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        chars = mainActivity.etText.getText().toString();
        schema = mainActivity.sSchema.getSelectedItem().toString();
        url = mainActivity.sURL.getSelectedItem().toString();
    }

    private String getDocument(URL url, String charset) {
        StringBuffer sb = new StringBuffer();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), charset))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private Queue<Queue<String>> getPronunciationsOfMultipleCharacters(StringBuilder cjkuis) throws MalformedURLException, UnsupportedEncodingException {
        String document = "";
        Queue<String> character;
        Matcher pronsMatcher;
        Queue<Queue<String>> prons = new ArrayDeque<>();
        switch (url) {
            case URLs.CUHK:
                document = getDocument(new URL(CUHK + URLEncoder.encode(cjkuis.toString(), BIG5)), BIG5);

                character = new ArrayDeque<>();

                pronsMatcher = PATTERN_CUHK_PRON.matcher(document);
                while (pronsMatcher.find())
                    character.offer(jyutpingToSpecifiedSchema(pronsMatcher.group()));

                prons.offer(character);

                break;
            case URLs.SHYYP:
                document = getDocument(new URL(SHYYP + URLEncoder.encode(cjkuis.toString(), UTF_8)), UTF_8);

                Matcher charMatcher = PATTERN_SHYYP_CHAR.matcher(document);
                while (charMatcher.find()) {
                    character = new ArrayDeque<>();

                    pronsMatcher = PATTERN_SHYYP_CHAR_PRON.matcher(charMatcher.group());
                    while (pronsMatcher.find())
                        character.offer(jyutpingToSpecifiedSchema(pronsMatcher.group()));

                    prons.offer(character);
                }

                break;
        }

        return prons;
    }

    private boolean isCjkui(char c) {
        return '\u4E00' <= c && c < '\uA000';
    }

    private String jyutpingToIpa(String jyutping) {
        return "["
                + jyutping
                .replaceAll("(?<=^|[^a-z])(?=a|e|o|uk|ung)", "ʔ")
                .replaceAll("eoi", "ɵy̯")
                .replaceAll("eo", "ɵ")
                .replaceAll("oe", "œː")
                .replaceAll("(?<=[aeou]i|[aeio]u)", "̯")
                .replaceAll("yu", "yː")
                .replaceAll("i(?=[umpnt]?\\d)", "iː")
                .replaceAll("u(?=[int]?\\d)", "uː")
                .replaceAll("(?<=[^a])a(?=[^a])", "ɐ")
                .replaceAll("aa", "aː")
                .replaceAll("e(?=(?:u|m|ng|k)|\\d)", "ɛː")
                .replaceAll("o(?=(?:i|m|n|ng|k)|\\d)", "ɔː")
                .replaceAll("i(?=ng|k)", "e")
                .replaceAll("u(?=ng|k)", "o")
                .replaceAll("(?<=[gk])w", "ʷ")
                .replaceAll("(?<=^[ptk])", "ʰ")
                .replaceAll("ʰʷ", "ʷʰ")
                .replaceAll("c", "t͡sʰ")
                .replaceAll("(?<=[ptk])(?=\\d)", "̚")
                .replaceAll("b", "p")
                .replaceAll("d", "t")
                .replaceAll("z", "t͡s")
                .replaceAll("ng", "ŋ")
                .replaceAll("g", "k")
                .replaceAll("(?<=[ptk]̚)1", "˥")
                .replaceAll("1", "˥˧")
                .replaceAll("2", "˧˥")
                .replaceAll("3", "˧")
                .replaceAll("4", "˨˩")
                .replaceAll("5", "˦˥")
                .replaceAll("6", "˨")
                + "]";
    }

    private String jyutpingToSpecifiedSchema(String jyutping) {
        return jyutpingToSpecifiedSchema(jyutping, false);
    }

    private String jyutpingToSpecifiedSchema(String jyutping, boolean ipa) {
        switch (schema) {
            case Schemas.JYUTPING:
                return jyutping + (ipa ? " " + jyutpingToIpa(jyutping) : "");
            case Schemas.IPA:
                return jyutpingToIpa(jyutping);
        }
        return jyutping;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadPronunciations() {
        try {
            if (chars.length() == 1) {
                String document = "";
                Matcher explsMatcher, pronsMatcher;
                Queue<String> expls = new ArrayDeque<>(), prons = new ArrayDeque<>();
                switch (url) {
                    case URLs.CUHK:
                        document = getDocument(new URL(CUHK + URLEncoder.encode(chars, BIG5)), BIG5);

                        pronsMatcher = PATTERN_CUHK_PRON.matcher(document);
                        while (pronsMatcher.find())
                            prons.offer(jyutpingToSpecifiedSchema(pronsMatcher.group(), true));

                        explsMatcher = PATTERN_CUHK_EXPL.matcher(document);
                        while (explsMatcher.find())
                            expls.offer(explsMatcher.group().replaceAll("<.*?>", ""));

                        break;
                    case URLs.SHYYP:
                        document = getDocument(new URL(SHYYP_1 + URLEncoder.encode(chars, UTF_8)), UTF_8);

                        pronsMatcher = PATTERN_SHYYP_PRON.matcher(document);
                        while (pronsMatcher.find())
                            prons.offer(jyutpingToSpecifiedSchema(pronsMatcher.group(), true));

                        explsMatcher = PATTERN_SHYYP_EXPL.matcher(document);
                        while (explsMatcher.find())
                            expls.offer(explsMatcher.group().replaceAll("<.*?>", ""));

                        break;
                }

                if (document.length() == 0) {
                    prons.offer("無法加載網頁");
                    expls.offer("");
                } else if (prons.size() == 0) {
                    prons.offer("無結果");
                    expls.offer("");
                }

                int ps = prons.size(), es = expls.size();
                if (ps < es) {
                    prons.clear();
                    prons.offer("匹配讀音與解釋失敗");
                    expls.clear();
                    expls.offer(String.format("num of prons: %d, num of expls: %d", ps, es));
                }

                mainActivity.runOnUiThread(() -> {
                    while (!prons.isEmpty()) {
                        LinearLayout layout = (LinearLayout) mainActivity.layoutInflater.inflate(R.layout.result, null);
                        ((TextView) layout.findViewById(R.id.tv_pron)).setText(prons.poll());
                        if (!expls.isEmpty()) {
                            ((TextView) layout.findViewById(R.id.tv_expl)).setText(expls.poll());
                        }
                        mainActivity.llResult.addView(layout);
                    }
                });

            } else {

                final int length = chars.length(), charsLengthPerPage = CHARS_LENGTH_PER_PAGE.getOrDefault(url, 1);
                StringBuilder page = new StringBuilder(), pageOfCjkuis = new StringBuilder();
                for (int i = 0, charsLengthOfCurrPg = 0; i < length; ++i) {
                    char c = chars.charAt(i);
                    page.append(c);
                    if (isCjkui(c)) {
                        pageOfCjkuis.append(c);

                        if (++charsLengthOfCurrPg >= charsLengthPerPage) {
                            charsLengthOfCurrPg = 0;

                            int progr = i;
                            mainActivity.runOnUiThread(() ->
                                    mainActivity.bQuery.setText(String.format(LOADING, progr * 100 / length)));

                            showPage(page.toString(), getPronunciationsOfMultipleCharacters(pageOfCjkuis));
                            pageOfCjkuis = new StringBuilder();
                            page = new StringBuilder();

                        }
                    }

                }
                if (page.length() > 0) {
                    showPage(page.toString(),
                            pageOfCjkuis.length() > 0 ? getPronunciationsOfMultipleCharacters(pageOfCjkuis) : null);
                }
            }


        } catch (UnsupportedEncodingException | MalformedURLException e) {

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public synchronized void run() {

        loadPronunciations();

        mainActivity.runOnUiThread(() -> {
            mainActivity.bQuery.setText("査詢");
            mainActivity.bQuery.setEnabled(true);
        });

    }

    private void showPage(String page, Queue<Queue<String>> pronunciations) {
        mainActivity.runOnUiThread(() -> {
            LinearLayout result = mainActivity.llResult, currentLine;
            int resultChildCount = result.getChildCount();
            if (resultChildCount == 0) {
                LinearLayout line = new LinearLayout(mainActivity);
                line.setGravity(Gravity.BOTTOM);
                result.addView(line);
                ++resultChildCount;
            }
            currentLine = (LinearLayout) result.getChildAt(resultChildCount - 1);
            for (int i = 0; i < page.length(); ++i) {
                char c = page.charAt(i);
                LinearLayout character = new LinearLayout(mainActivity);
                character.setGravity(Gravity.CENTER_HORIZONTAL);
                character.setOrientation(LinearLayout.VERTICAL);
                TextView tv = new TextView(mainActivity);
                tv.setLayoutParams(LAYOUT_PARAMS);
                tv.setTextAppearance(androidx.appcompat.R.style.TextAppearance_AppCompat_Large);
                tv.setText(String.valueOf(c));
                if (isCjkui(c)) {
                    Queue<String> prons = pronunciations.poll();
                    while (prons != null && !prons.isEmpty()) {
                        TextView tvPron = new TextView(mainActivity);
                        tvPron.setLayoutParams(LAYOUT_PARAMS);
                        tvPron.setText(prons.poll());
                        character.addView(tvPron);
                    }
                }
                character.addView(tv);
                currentLine.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                character.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                if (currentLine.getMeasuredWidth() + character.getMeasuredWidth() > result.getWidth()) {
                    currentLine = new LinearLayout(mainActivity);
                    currentLine.setGravity(Gravity.BOTTOM);
                    result.addView(currentLine);
                }
                currentLine.addView(character);
            }
        });
    }
}
