[![Download](https://api.bintray.com/packages/ricky12awesome/github/json-schema-serialization/images/download.svg)](https://bintray.com/ricky12awesome/github/json-schema-serialization/_latestVersion)
[![](https://jitpack.io/v/Ricky12Awesome/json-schema-serialization.svg)](https://jitpack.io/#Ricky12Awesome/json-schema-serialization)

# json-schema-serialization (jss)
Adds support for Json Schema using [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)

Currently, Json Schema DSL is very experimental, expect a lot of changes.

Dependency
----------
You would need [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) setup to use this dependency
##### Gradle / Gradle Kotlin DSL

```kotlin
repositories {
  jcenter()  
  // or maven("https://dl.bintray.com/ricky12awesome/github")
}

dependencies {
  implementation("com.github.Ricky12Awesome:json-schema-serialization:0.6.6")
}
```

Usage
-----
![](https://i.imgur.com/PwPEMAw.gif)

Code
----
```kotlin
@Serializable
data class Config(
  @Description(["Name for this project."])
  val name: String = "",
  @Description(["Theme for this project."])
  val theme: Theme = Theme()
)

@Serializable
sealed class ThemeColor {
  @Serializable @SerialName("HEX")
  data class HEX(
    @Pattern("#[0-9a-fA-F]{2,6}") val hex: String
  ) : ThemeColor()

  @Serializable @SerialName("RGB")
  data class RGB(
    @IntRange(0, 255) val r: Int,
    @IntRange(0, 255) val g: Int,
    @IntRange(0, 255) val b: Int
  ) : ThemeColor()

  @Serializable @SerialName("HSV")
  data class HSV(
    @IntRange(1, 360) val h: Int,
    @FloatRange(0.0, 1.0) val s: Double,
    @FloatRange(0.0, 1.0) val v: Double
  ) : ThemeColor()

  @Serializable @SerialName("HSL")
  data class HSL(
    @IntRange(1, 360) val h: Int,
    @FloatRange(0.0, 1.0) val s: Double,
    @FloatRange(0.0, 1.0) val l: Double
  ) : ThemeColor()
}

@Serializable
data class Theme(
  @Description(["Primary color for this theme."]) @Definition("ThemeColor")
  val primary: ThemeColor = ThemeColor.RGB(128, 128, 128),
  @Description(["Secondary color for this theme."]) @Definition("ThemeColor")
  val secondary: ThemeColor = ThemeColor.HSV(0, 0.0, 0.3),
  @Description(["Accent color for this theme."]) @Definition("ThemeColor")
  val accent: ThemeColor = ThemeColor.HSL(0, 0.0, 0.8),
  @Description(["Background color for this theme."]) @Definition("ThemeColor")
  val background: ThemeColor = ThemeColor.HEX("#242424")
)
```
Schema
------
```kotlin
globalJson.encodeToSchema(Config.serializer(), generateDefinitions = false)
```
```json
{
  "$schema": "http://json-schema.org/draft-07/schema",
  "type": "object",
  "properties": {
    "name": {
      "type": "string",
      "description": "Name for this project."
    },
    "theme": {
      "type": "object",
      "properties": {
        "primary": {
          "description": "Primary color for this theme.",
          "$ref": "#/definitions/ThemeColor"
        },
        "secondary": {
          "description": "Secondary color for this theme.",
          "$ref": "#/definitions/ThemeColor"
        },
        "accent": {
          "description": "Accent color for this theme.",
          "$ref": "#/definitions/ThemeColor"
        },
        "background": {
          "description": "Background color for this theme.",
          "$ref": "#/definitions/ThemeColor"
        }
      }
    }
  },
  "definitions": {
    "ThemeColor": {
      "properties": {
        "type": {
          "type": "string",
          "enum": [
            "HEX",
            "RGB",
            "HSV",
            "HSL"
          ]
        }
      },
      "anyOf": [
        {
          "type": "object",
          "properties": {
            "type": {
              "const": "HEX"
            },
            "hex": {
              "type": "string",
              "pattern": "#[0-9a-fA-F]{2,6}"
            }
          },
          "required": [
            "hex"
          ]
        },
        {
          "type": "object",
          "properties": {
            "type": {
              "const": "RGB"
            },
            "r": {
              "type": "number",
              "minimum": 0,
              "maximum": 255
            },
            "g": {
              "type": "number",
              "minimum": 0,
              "maximum": 255
            },
            "b": {
              "type": "number",
              "minimum": 0,
              "maximum": 255
            }
          },
          "required": [
            "r",
            "g",
            "b"
          ]
        },
        {
          "type": "object",
          "properties": {
            "type": {
              "const": "HSV"
            },
            "h": {
              "type": "number",
              "minimum": 1,
              "maximum": 360
            },
            "s": {
              "type": "number",
              "minimum": 0.0,
              "maximum": 1.0
            },
            "v": {
              "type": "number",
              "minimum": 0.0,
              "maximum": 1.0
            }
          },
          "required": [
            "h",
            "s",
            "v"
          ]
        },
        {
          "type": "object",
          "properties": {
            "type": {
              "const": "HSL"
            },
            "h": {
              "type": "number",
              "minimum": 1,
              "maximum": 360
            },
            "s": {
              "type": "number",
              "minimum": 0.0,
              "maximum": 1.0
            },
            "l": {
              "type": "number",
              "minimum": 0.0,
              "maximum": 1.0
            }
          },
          "required": [
            "h",
            "s",
            "l"
          ]
        }
      ],
      "required": [
        "type"
      ]
    }
  }
}
```

Some Features I'm thinking about adding
---------------------------------------

- Json Schema DSL
- Json Schema Data Object instead of using `JsonObject`

