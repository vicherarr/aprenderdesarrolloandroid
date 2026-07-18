package com.aprender.holaandroid.data.remote

import com.aprender.holaandroid.data.repository.DefaultFraseRepository
import com.aprender.holaandroid.di.RedModule
import com.aprender.holaandroid.domain.usecase.ObtenerFraseUseCase
import com.aprender.holaandroid.testutil.FakeFraseDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Hasta ahora los fakes sustituían a FrasesApi: NADA testeaba el stack real
 * de Retrofit + kotlinx.serialization. MockWebServer cierra ese hueco: un
 * servidor HTTP de verdad en el test, con Retrofit apuntando a él.
 *
 * Clave: el Json es EL DE PRODUCCIÓN (RedModule.proveeJson()). Si alguien
 * borra ignoreUnknownKeys de RedModule, estos tests se ponen rojos.
 */
class FrasesApiTest {

    private lateinit var servidor: MockWebServer
    private lateinit var api: FrasesApi

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        api = Retrofit.Builder()
            .baseUrl(servidor.url("/"))   // en vez de dummyjson.com
            .client(OkHttpClient())
            .addConverterFactory(
                RedModule.proveeJson().asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(FrasesApi::class.java)
    }

    @After
    fun apagar() {
        servidor.shutdown()
    }

    private fun encola(cuerpo: String, codigo: Int = 200) {
        servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))
    }

    @Test
    fun `el JSON real de la API se parsea al DTO via SerialName`() = runTest {
        encola("""{"id":254,"quote":"Do not sit and wait.","author":"Rumi"}""")

        val dto = api.obtenerFraseAleatoria()

        assertEquals(FraseDto(id = 254, texto = "Do not sit and wait.", autor = "Rumi"), dto)
        // También se afirma sobre la PETICIÓN: la anotación @GET construyó la ruta
        assertEquals("/quotes/random", servidor.takeRequest().path)
    }

    @Test
    fun `campos desconocidos no rompen el parseo (protege ignoreUnknownKeys)`() = runTest {
        encola("""{"id":1,"quote":"q","author":"a","categoria":"nueva","likes":42}""")

        val dto = api.obtenerFraseAleatoria()

        assertEquals(1, dto.id)
    }

    @Test(expected = SerializationException::class)
    fun `si falta un campo obligatorio, excepcion de serializacion`() = runTest {
        encola("""{"id":1,"quote":"sin autor"}""")

        api.obtenerFraseAleatoria()
    }

    @Test
    fun `un 500 del servidor llega como HttpException con su codigo`() = runTest {
        encola("""{"message":"Internal Server Error"}""", codigo = 500)

        try {
            api.obtenerFraseAleatoria()
            throw AssertionError("Debió lanzar HttpException")
        } catch (e: HttpException) {
            assertEquals(500, e.code())
        }
    }

    /**
     * Integración de verdad: servidor HTTP → Retrofit → repositorio → caso
     * de uso. El 500 acaba siendo el Result.failure que la UI convierte en
     * mensaje (guía 06), y el camino feliz persiste en el DAO (guía 07).
     */
    @Test
    fun `de un 500 del servidor al Result de dominio, capas pegadas`() = runTest {
        encola("error", codigo = 500)
        val useCase = ObtenerFraseUseCase(DefaultFraseRepository(api, FakeFraseDao()))

        val resultado = useCase()

        assertTrue(resultado.exceptionOrNull() is HttpException)
    }

    @Test
    fun `del JSON del servidor a la frase guardada, capas pegadas`() = runTest {
        encola("""{"id":9,"quote":"q","author":"a"}""")
        val dao = FakeFraseDao()
        val useCase = ObtenerFraseUseCase(DefaultFraseRepository(api, dao))

        val resultado = useCase()

        assertEquals("a", resultado.getOrThrow().autor)
        assertEquals(9, dao.observarTodas().first().single().id)
    }
}
