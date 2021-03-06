/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.moshi

import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Assert.fail
import org.junit.Test
import java.util.Locale

class GeneratedAdaptersTest {

  private val moshi = Moshi.Builder().build()

  @Test
  fun jsonAnnotation() {
    val adapter = moshi.adapter(JsonAnnotation::class.java)

    // Read
    @Language("JSON")
    val json = """{"foo": "bar"}"""

    val instance = adapter.fromJson(json)!!
    assertThat(instance.bar).isEqualTo("bar")

    // Write
    @Language("JSON")
    val expectedJson = """{"foo":"baz"}"""

    assertThat(adapter.toJson(JsonAnnotation("baz"))).isEqualTo(expectedJson)
  }

  @JsonClass(generateAdapter = true)
  data class JsonAnnotation(@Json(name = "foo") val bar: String)

  @Test
  fun defaultValues() {
    val adapter = moshi.adapter(DefaultValues::class.java)

    // Read/write with default values
    @Language("JSON")
    val json = """{"foo":"fooString"}"""

    val instance = adapter.fromJson(json)!!
    assertThat(instance.foo).isEqualTo("fooString")
    assertThat(instance.bar).isEqualTo("")
    assertThat(instance.nullableBar).isNull()
    assertThat(instance.bazList).apply {
      isNotNull()
      isEmpty()
    }

    @Language("JSON") val expected = """{"foo":"fooString","bar":"","bazList":[]}"""
    assertThat(adapter.toJson(DefaultValues("fooString"))).isEqualTo(expected)

    // Read/write with real values
    @Language("JSON")
    val json2 = """
      {"foo":"fooString","bar":"barString","nullableBar":"bar","bazList":["baz"]}
      """.trimIndent()

    val instance2 = adapter.fromJson(json2)!!
    assertThat(instance2.foo).isEqualTo("fooString")
    assertThat(instance2.bar).isEqualTo("barString")
    assertThat(instance2.nullableBar).isEqualTo("bar")
    assertThat(instance2.bazList).containsExactly("baz")
    assertThat(adapter.toJson(instance2)).isEqualTo(json2)
  }

  @JsonClass(generateAdapter = true)
  data class DefaultValues(val foo: String,
      val bar: String = "",
      val nullableBar: String? = null,
      val bazList: List<String> = emptyList())

  @Test
  fun nullableArray() {
    val adapter = moshi.adapter(NullableArray::class.java)

    @Language("JSON")
    val json = """{"data":[null,"why"]}"""

    val instance = adapter.fromJson(json)!!
    assertThat(instance.data).containsExactly(null, "why")
    assertThat(adapter.toJson(instance)).isEqualTo(json)
  }

  @JsonClass(generateAdapter = true)
  data class NullableArray(val data: Array<String?>)

  @Test
  fun primitiveArray() {
    val adapter = moshi.adapter(PrimitiveArray::class.java)

    @Language("JSON")
    val json = """{"ints":[0,1]}"""

    val instance = adapter.fromJson(json)!!
    assertThat(instance.ints).containsExactly(0, 1)
    assertThat(adapter.toJson(instance)).isEqualTo(json)
  }

  @JsonClass(generateAdapter = true)
  data class PrimitiveArray(val ints: IntArray)

  @Test
  fun nullableTypes() {
    val adapter = moshi.adapter(NullabeTypes::class.java)

    @Language("JSON")
    val json = """{"foo":"foo","nullableString":null}"""
    @Language("JSON")
    val invalidJson = """{"foo":null,"nullableString":null}"""

    val instance = adapter.fromJson(json)!!
    assertThat(instance.foo).isEqualTo("foo")
    assertThat(instance.nullableString).isNull()

    try {
      adapter.fromJson(invalidJson)
      fail("The invalid json should have failed!")
    } catch (e: JsonDataException) {
      assertThat(e).hasMessageContaining("foo")
    }
  }

  @JsonClass(generateAdapter = true)
  data class NullabeTypes(
      val foo: String,
      val nullableString: String?
  )

  @Test
  fun collections() {
    val adapter = moshi.adapter(SpecialCollections::class.java)

    val specialCollections = SpecialCollections(
        mutableListOf(),
        mutableSetOf(),
        mutableMapOf(),
        emptyList(),
        emptySet(),
        emptyMap()
    )

    val json = adapter.toJson(specialCollections)
    val newCollections = adapter.fromJson(json)
    assertThat(newCollections).isEqualTo(specialCollections)
  }

  @JsonClass(generateAdapter = true)
  data class SpecialCollections(
      val mutableList: MutableList<String>,
      val mutableSet: MutableSet<String>,
      val mutableMap: MutableMap<String, String>,
      val immutableList: List<String>,
      val immutableSet: Set<String>,
      val immutableMap: Map<String, String>
  )

  @Test
  fun mutableProperties() {
    val adapter = moshi.adapter(MutableProperties::class.java)

    val mutableProperties = MutableProperties(
        "immutableProperty",
        "mutableProperty",
        mutableListOf("immutableMutableList"),
        mutableListOf("immutableImmutableList"),
        mutableListOf("mutableMutableList"),
        mutableListOf("mutableImmutableList"),
        "immutableProperty",
        "mutableProperty",
        mutableListOf("immutableMutableList"),
        mutableListOf("immutableImmutableList"),
        mutableListOf("mutableMutableList"),
        mutableListOf("mutableImmutableList")
    )

    val json = adapter.toJson(mutableProperties)
    val newMutableProperties = adapter.fromJson(json)
    assertThat(newMutableProperties).isEqualTo(mutableProperties)
  }

  @JsonClass(generateAdapter = true)
  data class MutableProperties(
      val immutableProperty: String,
      var mutableProperty: String,
      val immutableMutableList: MutableList<String>,
      val immutableImmutableList: List<String>,
      var mutableMutableList: MutableList<String>,
      var mutableImmutableList: List<String>,
      val nullableImmutableProperty: String?,
      var nullableMutableProperty: String?,
      val nullableImmutableMutableList: MutableList<String>?,
      val nullableImmutableImmutableList: List<String>?,
      var nullableMutableMutableList: MutableList<String>?,
      var nullableMutableImmutableList: List<String>
  )

  @Test
  fun nullableTypeParams() {
    val adapter = moshi.adapter<NullableTypeParams<Int>>(
        Types.newParameterizedType(NullableTypeParams::class.java, Int::class.javaObjectType))
    val nullSerializing = adapter.serializeNulls()

    val nullableTypeParams = NullableTypeParams<Int>(
        listOf("foo", null, "bar"),
        setOf("foo", null, "bar"),
        mapOf("foo" to "bar", "baz" to null),
        null
    )

    val noNullsTypeParams = NullableTypeParams<Int>(
        nullableTypeParams.nullableList,
        nullableTypeParams.nullableSet,
        nullableTypeParams.nullableMap.filterValues { it != null },
        null
    )

    val json = adapter.toJson(nullableTypeParams)
    val newNullableTypeParams = adapter.fromJson(json)
    assertThat(newNullableTypeParams).isEqualTo(noNullsTypeParams)

    val nullSerializedJson = nullSerializing.toJson(nullableTypeParams)
    val nullSerializedNullableTypeParams = adapter.fromJson(nullSerializedJson)
    assertThat(nullSerializedNullableTypeParams).isEqualTo(nullableTypeParams)
  }

  @Test
  fun doNotGenerateAdapter() {
    try {
      StandardJsonAdapters.generatedAdapter(
          moshi, DoNotGenerateAdapter::class.java, DoNotGenerateAdapter::class.java)
      fail("found a generated adapter for a type that shouldn't have one")
    } catch (e: RuntimeException) {
      assertThat(e).hasCauseInstanceOf(ClassNotFoundException::class.java)
    }
  }

  @JsonClass(generateAdapter = false)
  data class DoNotGenerateAdapter(val foo: String)

  @Test fun constructorParameters() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ConstructorParameters::class.java)

    val encoded = ConstructorParameters(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class ConstructorParameters(var a: Int, var b: Int)

  @Test fun properties() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(Properties::class.java)

    val encoded = Properties()
    encoded.a = 3
    encoded.b = 5
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":3,"b":5}""")!!
    assertThat(decoded.a).isEqualTo(3)
    assertThat(decoded.b).isEqualTo(5)
  }

  @JsonClass(generateAdapter = true)
  class Properties {
    var a: Int = -1
    var b: Int = -1
  }

  @Test fun constructorParametersAndProperties() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ConstructorParametersAndProperties::class.java)

    val encoded = ConstructorParametersAndProperties(3)
    encoded.b = 5
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class ConstructorParametersAndProperties(var a: Int) {
    var b: Int = -1
  }

  @Test fun immutableConstructorParameters() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ImmutableConstructorParameters::class.java)

    val encoded = ImmutableConstructorParameters(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class ImmutableConstructorParameters(val a: Int, val b: Int)

  @Test fun immutableProperties() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ImmutableProperties::class.java)

    val encoded = ImmutableProperties(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":3,"b":5}""")!!
    assertThat(decoded.a).isEqualTo(3)
    assertThat(decoded.b).isEqualTo(5)
  }

  @JsonClass(generateAdapter = true)
  class ImmutableProperties(a: Int, b: Int) {
    val a = a
    val b = b
  }

  @Test fun constructorDefaults() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ConstructorDefaultValues::class.java)

    val encoded = ConstructorDefaultValues(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"b":6}""")!!
    assertThat(decoded.a).isEqualTo(-1)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class ConstructorDefaultValues(var a: Int = -1, var b: Int = -2)

  @Test fun requiredValueAbsent() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(RequiredValueAbsent::class.java)

    try {
      jsonAdapter.fromJson("""{"a":4}""")
      fail()
    } catch(expected: JsonDataException) {
      assertThat(expected).hasMessage("Required property 'b' missing at \$")
    }
  }

  @JsonClass(generateAdapter = true)
  class RequiredValueAbsent(var a: Int = 3, var b: Int)

  @Test fun nonNullConstructorParameterCalledWithNullFailsWithJsonDataException() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(HasNonNullConstructorParameter::class.java)

    try {
      jsonAdapter.fromJson("{\"a\":null}")
      fail()
    } catch (expected: JsonDataException) {
      assertThat(expected).hasMessage("Required property 'a' missing at \$")
    }
  }

  @JsonClass(generateAdapter = true)
  class HasNonNullConstructorParameter(val a: String)

  @Test fun explicitNull() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ExplicitNull::class.java)

    val encoded = ExplicitNull(null, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"b":5}""")
    assertThat(jsonAdapter.serializeNulls().toJson(encoded)).isEqualTo("""{"a":null,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":null,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(null)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class ExplicitNull(var a: Int?, var b: Int?)

  @Test fun absentNull() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(AbsentNull::class.java)

    val encoded = AbsentNull(null, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"b":5}""")
    assertThat(jsonAdapter.serializeNulls().toJson(encoded)).isEqualTo("""{"a":null,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"b":6}""")!!
    assertThat(decoded.a).isNull()
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class AbsentNull(var a: Int?, var b: Int?)

  @Test fun constructorParameterWithQualifier() {
    val moshi = Moshi.Builder()
        .add(UppercaseJsonAdapter())
        .build()
    val jsonAdapter = moshi.adapter(ConstructorParameterWithQualifier::class.java)

    val encoded = ConstructorParameterWithQualifier("Android", "Banana")
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":"ANDROID","b":"Banana"}""")

    val decoded = jsonAdapter.fromJson("""{"a":"Android","b":"Banana"}""")!!
    assertThat(decoded.a).isEqualTo("android")
    assertThat(decoded.b).isEqualTo("Banana")
  }

  @JsonClass(generateAdapter = true)
  class ConstructorParameterWithQualifier(@Uppercase var a: String, var b: String)

  @Test fun propertyWithQualifier() {
    val moshi = Moshi.Builder()
        .add(UppercaseJsonAdapter())
        .build()
    val jsonAdapter = moshi.adapter(PropertyWithQualifier::class.java)

    val encoded = PropertyWithQualifier()
    encoded.a = "Android"
    encoded.b = "Banana"
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"a":"ANDROID","b":"Banana"}""")

    val decoded = jsonAdapter.fromJson("""{"a":"Android","b":"Banana"}""")!!
    assertThat(decoded.a).isEqualTo("android")
    assertThat(decoded.b).isEqualTo("Banana")
  }

  @JsonClass(generateAdapter = true)
  class PropertyWithQualifier {
    @Uppercase var a: String = ""
    var b: String = ""
  }

  @Test fun constructorParameterWithJsonName() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(ConstructorParameterWithJsonName::class.java)

    val encoded = ConstructorParameterWithJsonName(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"key a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"key a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class ConstructorParameterWithJsonName(@Json(name = "key a") var a: Int, var b: Int)

  @Test fun propertyWithJsonName() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(PropertyWithJsonName::class.java)

    val encoded = PropertyWithJsonName()
    encoded.a = 3
    encoded.b = 5
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"key a":3,"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"key a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(4)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class PropertyWithJsonName {
    @Json(name = "key a") var a: Int = -1
    var b: Int = -1
  }

  @Test fun transientConstructorParameter() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(TransientConstructorParameter::class.java)

    val encoded = TransientConstructorParameter(3, 5)
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(-1)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class TransientConstructorParameter(@Transient var a: Int = -1, var b: Int = -1)

  @Test fun transientProperty() {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(TransientProperty::class.java)

    val encoded = TransientProperty()
    encoded.a = 3
    encoded.b = 5
    assertThat(jsonAdapter.toJson(encoded)).isEqualTo("""{"b":5}""")

    val decoded = jsonAdapter.fromJson("""{"a":4,"b":6}""")!!
    assertThat(decoded.a).isEqualTo(-1)
    assertThat(decoded.b).isEqualTo(6)
  }

  @JsonClass(generateAdapter = true)
  class TransientProperty {
    @Transient var a: Int = -1
    var b: Int = -1
  }

  @Retention(AnnotationRetention.RUNTIME)
  @JsonQualifier
  annotation class Uppercase

  class UppercaseJsonAdapter {
    @ToJson fun toJson(@Uppercase s: String) : String {
      return s.toUpperCase(Locale.US)
    }
    @FromJson @Uppercase fun fromJson(s: String) : String {
      return s.toLowerCase(Locale.US)
    }
  }
}

// Has to be outside to avoid Types seeing an owning class
@JsonClass(generateAdapter = true)
data class NullableTypeParams<T>(
    val nullableList: List<String?>,
    val nullableSet: Set<String?>,
    val nullableMap: Map<String, String?>,
    val nullableT: T?
)

typealias TypeAliasName = String

/**
 * This is here mostly just to ensure it still compiles. Covers variance, @Json, default values,
 * nullability, primitive arrays, and some wacky generics.
 */
@JsonClass(generateAdapter = true)
data class SmokeTestType(
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String,
    val age: Int,
    val nationalities: List<String> = emptyList(),
    val weight: Float,
    val tattoos: Boolean = false,
    val race: String?,
    val hasChildren: Boolean = false,
    val favoriteFood: String? = null,
    val favoriteDrink: String? = "Water",
    val wildcardOut: List<out String> = emptyList(),
    val wildcardIn: Array<in String>,
    val any: List<*>,
    val anyTwo: List<Any>,
    val anyOut: List<out Any>,
    val favoriteThreeNumbers: IntArray,
    val favoriteArrayValues: Array<String>,
    val favoriteNullableArrayValues: Array<String?>,
    val nullableSetListMapArrayNullableIntWithDefault: Set<List<Map<String, Array<IntArray?>>>>? = null,
    val aliasedName: TypeAliasName = "Woah"
)
