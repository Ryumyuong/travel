package com.hehe.travel

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class PlaceAdapter : JsonDeserializer<Place> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Place {
        return when {
            json.isJsonPrimitive -> {
                // "명동" 같은 문자열 -> 객체로 변환
                Place(name = json.asString, description = null)
            }
            json.isJsonObject -> {
                val obj = json.asJsonObject
                Place(
                    name = obj["name"]?.asString ?: "",
                    description = obj["description"]?.asString
                )
            }
            else -> Place(name = "", description = null)
        }
    }
}