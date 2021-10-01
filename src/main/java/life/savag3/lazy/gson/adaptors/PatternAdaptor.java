package life.savag3.lazy.gson.adaptors;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.regex.Pattern;

public class PatternAdaptor implements JsonSerializer<Pattern>, JsonDeserializer<Pattern> {

    private final String PATTERN = "pattern";

    @Override
    public JsonElement serialize(Pattern pattern, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject object = new JsonObject();
        object.add(PATTERN, new JsonPrimitive(pattern.pattern()));
        return object;
    }

    @Override
    public Pattern deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
        JsonObject object = jsonElement.getAsJsonObject();
        return Pattern.compile(object.get(PATTERN).getAsString());
    }
}