import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.xmlpull.v1.XmlPullParserFactory
import java.util.concurrent.TimeUnit

/*
 * 网络请求相关
 *
 * @author Ysj
 * Create time: 2021/7/18
 */

val json by lazy { GsonBuilder().create() }

val xml by lazy { XmlPullParserFactory.newInstance() }

val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(3, TimeUnit.SECONDS)
    .readTimeout(3, TimeUnit.SECONDS)
    .writeTimeout(3, TimeUnit.SECONDS)
    .build()

fun requestGet(url: String) = okHttpClient.newCall(
    Request.Builder().url(url).get().build()
)

fun requestPost(url: String, requestBody: RequestBody) = okHttpClient.newCall(
    Request.Builder().url(url).post(requestBody).build()
)

inline fun <reified T> Response.result(): T? = if (!isSuccessful) null else body?.string()?.let {
    json.fromJson(it, T::class.java)
}