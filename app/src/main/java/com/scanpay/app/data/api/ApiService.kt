package com.scanpay.app.data.api

import com.scanpay.app.data.model.Vendor
import com.scanpay.app.data.model.QRCode
import com.scanpay.app.data.request.*
import com.scanpay.app.data.response.*
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // Auth
    @Headers("No-Authentication: true")
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @Headers("No-Authentication: true")
    @POST("auth/register/user")
    fun registerCustomer(@Body request: CustomerRegisterRequest): Call<AuthResponse>

    @Headers("No-Authentication: true")
    @POST("auth/register/vendor")
    fun registerVendor(@Body request: MerchantRegisterRequest): Call<VendorAuthResponse>

    @POST("auth/logout")
    fun logout(): Call<MessageResponse>

    @Headers("No-Authentication: true")
    @POST("auth/forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<ForgotPasswordResponse>

    @Headers("No-Authentication: true")
    @POST("auth/reset-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<MessageResponse>

    // QR Operations
    @POST("qr/generate")
    fun generateQRCode(@Body request: GenerateQRRequest): Call<GenerateQRResponse>

    @POST("qr/scan")
    fun scanQRCode(@Body request: ScanQRRequest): Call<QRScanResponse>

    // Payment Operations
    @POST("payment/initiate")
    fun initiatePayment(@Body request: PaymentRequest, @Header("Idempotency-Key") key: String): Call<TransactionResponse>

    @GET("merchant/qr")
    fun getMerchantQRCodes(): Call<List<QRCode>>

    @GET("payment/{transaction_id}/status")
    fun checkPaymentStatus(@Path("transaction_id") transactionId: Long): Call<PaymentStatusResponse>

    // User Operations
    @GET("user/profile")
    fun getUserProfile(): Call<AuthResponse>

    @GET("user/transactions")
    fun getUserTransactions(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50
    ): Call<List<TransactionResponse>>

    @GET("user/analytics")
    fun getUserAnalytics(@Query("days") days: Int = 30): Call<AnalyticsResponse>

    // Merchant Operations
    @GET("merchant/profile")
    fun getMerchantProfile(): Call<Vendor>

    @GET("merchant/transactions")
    fun getMerchantTransactions(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50
    ): Call<List<TransactionResponse>>

    @GET("merchant/analytics")
    fun getMerchantAnalytics(@Query("days") days: Int = 30): Call<AnalyticsResponse>

    // Top up
    @GET("topup/balance")
    fun getTopUpBalance(): Call<TopUpBalanceResponse>

    @POST("topup/initiate")
    fun initiateTopUp(@Body request: TopUpRequest): Call<TopUpResponse>
}

