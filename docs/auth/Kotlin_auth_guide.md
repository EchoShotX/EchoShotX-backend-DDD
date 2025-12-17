
## Kotlin (Android) 클라이언트 구현 가이드

### 1. AndroidManifest.xml 설정

딥링크 처리를 위해 `AndroidManifest.xml`에 Intent Filter 추가:

```xml
<activity
    android:name=".ui.auth.AuthCallbackActivity"
    android:exported="true"
    android:launchMode="singleTop">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="echoshotx"
            android:host="auth"
            android:pathPrefix="/callback" />
    </intent-filter>
</activity>
```

### 2. 의존성 추가 (build.gradle.kts)

```kotlin
dependencies {
    // Custom Tabs 지원
    implementation("androidx.browser:browser:1.7.0")
    
    // HTTP 통신 (Retrofit 또는 Ktor)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### 3. 딥링크 처리 Activity

**파일**: `app/src/main/java/com/echoshotx/ui/auth/AuthCallbackActivity.kt`

```kotlin
package com.echoshotx.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AuthCallbackActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleDeepLink(intent)
        finish() // 딥링크 처리 후 Activity 종료
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleDeepLink(it) }
        finish()
    }
    
    private fun handleDeepLink(intent: Intent) {
        val data: Uri? = intent.data
        val code = data?.getQueryParameter("code")
        
        if (code != null) {
            // ViewModel이나 Repository에 code 전달하여 토큰 교환
            lifecycleScope.launch {
                AuthRepository.exchangeCode(code) { result ->
                    result.onSuccess { tokenResponse ->
                        // 토큰 저장 및 메인 화면으로 이동
                        TokenManager.saveTokens(
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken
                        )
                        navigateToMain()
                    }.onFailure { error ->
                        // 에러 처리
                        handleAuthError(error)
                    }
                }
            }
        } else {
            handleAuthError(Exception("인증 코드를 받지 못했습니다."))
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
    
    private fun handleAuthError(error: Throwable) {
        // 에러 처리 로직 (에러 화면 표시 또는 로그인 화면으로 복귀)
    }
}
```

### 4. OAuth2 로그인 시작 (Custom Tab 사용)

**파일**: `app/src/main/java/com/echoshotx/ui/auth/AuthRepository.kt`

```kotlin
package com.echoshotx.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat

object AuthRepository {
    
    private const val BASE_URL = "https://your-ec2-domain.com"
    
    /**
     * OAuth2 로그인 시작
     * Custom Tab으로 OAuth2 인증 페이지 열기
     */
    fun startOAuth2Login(context: Context, provider: OAuthProvider) {
        val authUrl = buildAuthUrl(provider)
        
        val customTabsIntent = CustomTabsIntent.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.primary))
            .setShowTitle(true)
            .setStartAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
            .setExitAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
            .build()
        
        customTabsIntent.launchUrl(context, Uri.parse(authUrl))
    }
    
    private fun buildAuthUrl(provider: OAuthProvider): String {
        return "$BASE_URL/oauth2/authorization/${provider.name.lowercase()}?client=android"
    }
    
    /**
     * 인증 코드를 JWT 토큰으로 교환
     */
    suspend fun exchangeCode(
        code: String,
        callback: (Result<AuthTokenResponse>) -> Unit
    ) {
        try {
            val response = AuthApiService.exchangeCode(AuthExchangeRequest(code))
            if (response.isSuccessful && response.body() != null) {
                callback(Result.success(response.body()!!))
            } else {
                callback(Result.failure(Exception("토큰 교환 실패: ${response.message()}")))
            }
        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    }
}

enum class OAuthProvider {
    GOOGLE,
    NAVER,
    KAKAO
}
```

### 5. API 서비스 인터페이스

**파일**: `app/src/main/java/com/echoshotx/network/AuthApiService.kt`

```kotlin
package com.echoshotx.network

import com.echoshotx.ui.auth.AuthExchangeRequest
import com.echoshotx.ui.auth.AuthTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    
    @POST("auth/exchange")
    suspend fun exchangeCode(
        @Body request: AuthExchangeRequest
    ): Response<AuthTokenResponse>
}

// Request DTO
data class AuthExchangeRequest(
    val code: String
)

// Response DTO
data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)
```

### 6. Retrofit 설정

**파일**: `app/src/main/java/com/echoshotx/network/RetrofitClient.kt`

```kotlin
package com.echoshotx.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    
    private const val BASE_URL = "https://your-ec2-domain.com/"
    
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
}
```

### 7. 토큰 관리자

**파일**: `app/src/main/java/com/echoshotx/auth/TokenManager.kt`

```kotlin
package com.echoshotx.auth

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
        getPrefs(context).edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
    }
    
    fun getAccessToken(context: Context): String? {
        return getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
    }
    
    fun getRefreshToken(context: Context): String? {
        return getPrefs(context).getString(KEY_REFRESH_TOKEN, null)
    }
    
    fun clearTokens(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
    
    fun isLoggedIn(context: Context): Boolean {
        return getAccessToken(context) != null
    }
}
```

### 8. 로그인 화면에서 사용 예시

**파일**: `app/src/main/java/com/echoshotx/ui/auth/LoginFragment.kt`

```kotlin
package com.echoshotx.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class LoginFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 구글 로그인 버튼 클릭
        view.findViewById<View>(R.id.btnGoogleLogin).setOnClickListener {
            context?.let { ctx ->
                AuthRepository.startOAuth2Login(ctx, OAuthProvider.GOOGLE)
            }
        }
        
        // 네이버 로그인 버튼 클릭
        view.findViewById<View>(R.id.btnNaverLogin).setOnClickListener {
            context?.let { ctx ->
                AuthRepository.startOAuth2Login(ctx, OAuthProvider.NAVER)
            }
        }
        
        // 카카오 로그인 버튼 클릭
        view.findViewById<View>(R.id.btnKakaoLogin).setOnClickListener {
            context?.let { ctx ->
                AuthRepository.startOAuth2Login(ctx, OAuthProvider.KAKAO)
            }
        }
    }
}
```

### 9. API 요청 시 토큰 추가 (Interceptor 예시)

**파일**: `app/src/main/java/com/echoshotx/network/AuthInterceptor.kt`

```kotlin
package com.echoshotx.network

import android.content.Context
import com.echoshotx.auth.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val accessToken = TokenManager.getAccessToken(context)
        
        val authenticatedRequest = if (accessToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(authenticatedRequest)
    }
}
```

### 10. 에러 처리 및 재시도 로직

**파일**: `app/src/main/java/com/echoshotx/ui/auth/AuthErrorHandler.kt`

```kotlin
package com.echoshotx.ui.auth

import android.content.Context
import com.echoshotx.auth.TokenManager
import com.echoshotx.network.AuthApiService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object AuthErrorHandler {
    
    /**
     * 401 에러 발생 시 토큰 갱신 시도
     */
    suspend fun handleAuthError(
        context: Context,
        onRefreshSuccess: () -> Unit,
        onRefreshFailure: () -> Unit
    ) {
        val refreshToken = TokenManager.getRefreshToken(context)
        
        if (refreshToken != null) {
            try {
                // Refresh Token으로 새 Access Token 발급받는 API 호출
                // (서버에 Refresh Token 엔드포인트가 있다고 가정)
                // val newTokens = AuthApiService.refreshToken(refreshToken)
                // TokenManager.saveTokens(context, newTokens.accessToken, newTokens.refreshToken)
                // onRefreshSuccess()
                
                // TODO: 실제 refresh token API 호출 구현
                onRefreshFailure()
            } catch (e: Exception) {
                // Refresh 실패 시 로그인 화면으로 이동
                TokenManager.clearTokens(context)
                onRefreshFailure()
            }
        } else {
            onRefreshFailure()
        }
    }
}
```