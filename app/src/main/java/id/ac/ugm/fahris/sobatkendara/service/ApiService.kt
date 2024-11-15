package id.ac.ugm.fahris.sobatkendara.service

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object ApiService {
    // Toggle between different environments
    private const val ENVIRONMENT = "development" // or "production"

    private val BASE_URL: String
        get() = when (ENVIRONMENT) {
            "development" -> "https://192.168.26.128:8443"
            "production" -> "https://sk-service-414032994764.us-central1.run.app"
            else -> "https://192.168.26.128:8443"
        }

    suspend fun login(context: Context, email: String, password: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("${BASE_URL}/login")
                val httpsURLConnection = url.openConnection() as HttpsURLConnection

                if (ENVIRONMENT == "development") {
                    // Set up SSL to ignore certificate validation
                    httpsURLConnection.sslSocketFactory =
                        UnsafeSSLHelper.getUnsafeSSLSocketFactory()
                    httpsURLConnection.hostnameVerifier =
                        UnsafeSSLHelper.getUnsafeHostnameVerifier()
                }

                httpsURLConnection.requestMethod = "POST"
                httpsURLConnection.setRequestProperty("Content-Type", "application/json")
                httpsURLConnection.doOutput = true

                // Create the JSON request body
                val jsonInputString = JSONObject()
                jsonInputString.put("email", email)
                jsonInputString.put("password", password)

                // Write the request body
                val outputStream: OutputStream = httpsURLConnection.outputStream
                outputStream.write(jsonInputString.toString().toByteArray())
                outputStream.flush()
                outputStream.close()

                // Check the response code
                val responseCode = httpsURLConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the response
                    val reader = BufferedReader(InputStreamReader(httpsURLConnection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    // Parse the JSON response to extract the token
                    val jsonResponse = JSONObject(response)
                    return@withContext jsonResponse.getString("token")
                } else {
                    // Handle error
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Login failed: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                return@withContext null
            }
        }
    }
}