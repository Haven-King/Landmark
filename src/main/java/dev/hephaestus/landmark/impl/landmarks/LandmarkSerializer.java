package dev.hephaestus.landmark.impl.landmarks;

import com.google.gson.JsonElement;
import dev.hephaestus.landmark.api.Landmark;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.util.Identifier;

public class LandmarkSerializer {
	public static Landmark deserialize(JsonElement jsonElement) {
		Identifier id = new Identifier(jsonElement.getAsJsonObject().get("name").getAsString());
		LocationPredicate predicate = LocationPredicate.fromJson(jsonElement);
		return new Landmark(id, predicate);
	}
}
