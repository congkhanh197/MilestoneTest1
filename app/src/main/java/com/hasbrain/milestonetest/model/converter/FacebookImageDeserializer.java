package com.hasbrain.milestonetest.model.converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import com.hasbrain.milestonetest.model.FacebookImage;

import java.lang.reflect.Type;

/**
 * Created by Jupiter (vu.cao.duy@gmail.com) on 1/5/16.
 */
public class FacebookImageDeserializer extends EasyDeserializer<FacebookImage> {

    @Override
    public FacebookImage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        FacebookImage facebookImage = null;
        if (json != null && json.isJsonObject()) {
            facebookImage = new FacebookImage();
            JsonObject jsonObject = json.getAsJsonObject();
            facebookImage.setId(getStringValue(jsonObject.get("id"), null));
            facebookImage.setName(getStringValue(jsonObject.get("name"), null));
            facebookImage.setThumbnailUrl(getStringValue(jsonObject.get("picture"), null));
            facebookImage.setCreatedTime(getStringValue(jsonObject.get("created_time"), null));
            facebookImage.setHeight(getStringValue(jsonObject.get("height"), null));
            facebookImage.setWidth(getStringValue(jsonObject.get("width"), null));
            JsonElement authorJsonElement = jsonObject.get("from");
            if (authorJsonElement != null && authorJsonElement.isJsonObject()) {
                facebookImage.setFromUserName(getStringValue(authorJsonElement.getAsJsonObject().get("name"), null));
            }
            JsonElement imagesJsonEle = jsonObject.get("images");
            if (imagesJsonEle.isJsonArray()) {
                JsonArray images = imagesJsonEle.getAsJsonArray();
                facebookImage.setImageUrl(chooseImageFromArray(images));
            }
        }
        return facebookImage;
    }

    public String chooseImageFromArray(JsonArray images) {
        String imageUrl = null;
        int i = images.size() - 1;
        JsonObject imageJsonObject = images.get(i).getAsJsonObject();
        imageUrl = getStringValue(imageJsonObject.get("source"), null);
        return imageUrl;
    }
}
