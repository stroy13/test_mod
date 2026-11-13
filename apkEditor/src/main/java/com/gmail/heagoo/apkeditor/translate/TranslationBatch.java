package com.gmail.heagoo.apkeditor.translate;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import common.types.StringItem;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/** A strictly validated group of independent resource translations. */
public final class TranslationBatch {
    public static final int MAX_ITEMS = 3;
    public static final int MAX_CHARS = 150;
    public static final String SYSTEM = "Translate app interface strings naturally and concisely. Preserve placeholders, escapes and tags exactly. Return only the requested JSON.";
    private static final Gson GSON = new Gson();
    private TranslationBatch() {}

    public static boolean eligible(StringItem item) {
        return item != null && item.name != null && item.name.matches("[A-Za-z0-9_]{1,80}")
                && item.value != null && !item.value.isEmpty() && item.value.length() <= MAX_CHARS
                && item.styledValue == null && !item.value.startsWith("@") && !item.value.startsWith("?")
                && !TranslationSource.isInvariant(item.value);
    }
    public static String prompt(List<StringItem> items, String sourceQualifier, String target) {
        if (items.size() < 2 || items.size() > MAX_ITEMS) throw new IllegalArgumentException("Invalid group size");
        StringBuilder text = new StringBuilder("Translate each text ");
        String source = TranslationPrompt.sourceLanguage(sourceQualifier);
        if (source != null) text.append("from ").append(source).append(' ');
        text.append("into ").append(target).append(". Return a JSON array of objects with exactly id and text; keep every id. Translate the text, not the key.\n[");
        for (int i = 0; i < items.size(); i++) {
            StringItem item = items.get(i);
            if (!eligible(item)) throw new IllegalArgumentException("Invalid group item");
            if (i > 0) text.append(',');
            text.append("{\"id\":").append(i).append(",\"key\":").append(GSON.toJson(item.name))
                    .append(",\"text\":").append(GSON.toJson(item.value)).append('}');
        }
        return text.append("]\n/no_think").toString();
    }

    /** Reject anything ambiguous, incomplete or with changed resource placeholders. */
    public static List<String> parse(List<StringItem> items, String output) throws IOException {
        if (output == null || output.length() > 65536) throw new IOException("Invalid batch output");
        String[] values = new String[items.size()];
        try (JsonReader reader = new JsonReader(new StringReader(output))) {
            reader.setLenient(false);
            reader.beginArray();
            int count = 0;
            while (reader.hasNext()) {
                if (++count > items.size()) throw new IOException("Too many batch items");
                reader.beginObject();
                Integer id = null; String value = null;
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if ("id".equals(key) && id == null && reader.peek() == JsonToken.NUMBER) id = reader.nextInt();
                    else if ("text".equals(key) && value == null && reader.peek() == JsonToken.STRING) value = reader.nextString();
                    else throw new IOException("Unexpected batch field");
                }
                reader.endObject();
                if (id == null || id < 0 || id >= items.size() || value == null || values[id] != null) throw new IOException("Invalid batch ID or value");
                values[id] = TranslationOutput.clean(items.get(id).value, value);
                if (!TranslationReviewEdits.valid(items.get(id), values[id])) throw new IOException("Invalid batch resource text");
            }
            reader.endArray();
            if (reader.peek() != JsonToken.END_DOCUMENT || count != items.size()) throw new IOException("Incomplete batch output");
        } catch (IllegalStateException | NumberFormatException e) { throw new IOException("Invalid batch JSON", e); }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value == null) throw new IOException("Missing batch ID");
            result.add(value);
        }
        return result;
    }
}
