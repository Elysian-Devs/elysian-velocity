package org.elysian.velocity.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Utility for serializing and deserializing data for plugin messages
 * Uses JSON format for cross-platform compatibility
 */
object DataSerializer {

    private val gson = Gson()

    /**
     * Serialize map to JSON string
     */
    fun serialize(data: Map<String, Any>): String {
        return try {
            gson.toJson(data)
        } catch (e: Exception) {
            throw SerializationException("Failed to serialize data: ${e.message}", e)
        }
    }

    /**
     * Deserialize JSON string to map
     */
    fun deserialize(json: String): Map<String, Any> {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            throw SerializationException("Failed to deserialize data: ${e.message}", e)
        }
    }

    /**
     * Serialize object to JSON string
     */
    fun <T> serializeObject(obj: T): String {
        return try {
            gson.toJson(obj)
        } catch (e: Exception) {
            throw SerializationException("Failed to serialize object: ${e.message}", e)
        }
    }

    /**
     * Deserialize JSON string to object
     */
    fun <T> deserializeObject(json: String, clazz: Class<T>): T {
        return try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            throw SerializationException("Failed to deserialize object: ${e.message}", e)
        }
    }

    /**
     * Serialize list to JSON string
     */
    fun serializeList(list: List<Any>): String {
        return try {
            gson.toJson(list)
        } catch (e: Exception) {
            throw SerializationException("Failed to serialize list: ${e.message}", e)
        }
    }

    /**
     * Deserialize JSON string to list
     */
    fun deserializeList(json: String): List<Any> {
        return try {
            val type = object : TypeToken<List<Any>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            throw SerializationException("Failed to deserialize list: ${e.message}", e)
        }
    }

    /**
     * Check if string is valid JSON
     */
    fun isValidJson(json: String): Boolean {
        return try {
            gson.fromJson(json, Any::class.java)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Pretty print JSON
     */
    fun prettyPrint(json: String): String {
        return try {
            val obj = gson.fromJson(json, Any::class.java)
            com.google.gson.GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(obj)
        } catch (e: Exception) {
            json
        }
    }

    /**
     * Convert map to query string
     * Example: {a=1, b=2} -> "a=1&b=2"
     */
    fun mapToQueryString(data: Map<String, Any>): String {
        return data.entries.joinToString("&") { (key, value) ->
            "$key=${value.toString()}"
        }
    }

    /**
     * Parse query string to map
     * Example: "a=1&b=2" -> {a=1, b=2}
     */
    fun queryStringToMap(query: String): Map<String, String> {
        return query.split("&")
            .mapNotNull { pair ->
                val parts = pair.split("=")
                if (parts.size == 2) {
                    parts[0] to parts[1]
                } else {
                    null
                }
            }
            .toMap()
    }

    /**
     * Serialization exception
     */
    class SerializationException(message: String, cause: Throwable? = null) :
        Exception(message, cause)
}