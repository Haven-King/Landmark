package dev.hephaestus.landmark.impl.landmarks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hephaestus.landmark.api.LandmarkType;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.util.Identifier;

public class LandmarkSerializer {
	public static LandmarkType deserialize(Identifier id, JsonElement jsonElement) {
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		Identifier name_generator = new Identifier(jsonObject.get("name_generator").getAsString());
		LocationPredicate predicate = LocationPredicate.fromJson(jsonObject.get("location"));
		return new LandmarkType(id, name_generator, predicate);
	}
}
