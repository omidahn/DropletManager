package com.omiddd.dropletmanager.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.ParameterizedType

class DigitalOceanServiceReflectionTest {

    @Test
    fun suspendApiMethodsExposeParameterizedContinuation() {
        val methodNames = listOf("listDroplets", "listProjects", "listRegions")

        methodNames.forEach { name ->
            val method = DigitalOceanService::class.java.declaredMethods.first { it.name == name }
            val continuationType = method.genericParameterTypes.last()

            assertTrue(
                "$name must keep generic Continuation metadata for Retrofit",
                continuationType is ParameterizedType
            )

            val rawType = (continuationType as ParameterizedType).rawType
            assertEquals("kotlin.coroutines.Continuation", rawType.typeName)
        }
    }
}
