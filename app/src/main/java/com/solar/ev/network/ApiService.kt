package com.solar.ev.network

import com.solar.ev.model.DashboardStatsResponse
import com.solar.ev.model.DeleteApplicationResponse
import com.solar.ev.model.DetailDeleteResponse
import com.solar.ev.model.LoginRequest
import com.solar.ev.model.LoginResponse
import com.solar.ev.model.ProfileResponse
import com.solar.ev.model.SendVerificationEmailResponse
import com.solar.ev.model.SignupRequest
import com.solar.ev.model.SignupResponse
import com.solar.ev.model.UpdateProfileRequest
import com.solar.ev.model.UploadPhotoRequest
import com.solar.ev.model.UploadPhotoResponse
import com.solar.ev.model.application.ApplicationApplyRequest
import com.solar.ev.model.application.ApplicationApplyResponse
import com.solar.ev.model.application.ApplicationRequest
import com.solar.ev.model.application.ApplicationSuccessResponse
import com.solar.ev.model.application.ApplicationDetailResponse
import com.solar.ev.model.application.ApplicationListResponse
import com.solar.ev.model.bank.BankDetailFetchResponse
import com.solar.ev.model.bank.BankDetailsRequest
import com.solar.ev.model.bank.BankDetailsResponse
import com.solar.ev.model.bank.BankDetailsUpdateResponse
import com.solar.ev.model.common.DocumentVerificationRequest
import com.solar.ev.model.common.GenericVerificationResponse
import com.solar.ev.model.discom.DiscomDetailFetchResponse
import com.solar.ev.model.discom.DiscomDetailsRequest
import com.solar.ev.model.discom.DiscomDetailsResponse
import com.solar.ev.model.installation.InstallationDetailFetchResponse
import com.solar.ev.model.installation.InstallationDetailsRequest
import com.solar.ev.model.installation.InstallationDetailsResponse
import com.solar.ev.model.kyc.KYCSubmitRequest
import com.solar.ev.model.kyc.KYCSubmitResponse
import com.solar.ev.model.kyc.KYCFetchResponse
import com.solar.ev.model.kyc.KYCUpdateResponse
import com.solar.ev.model.quotation.CreateQuotationRequest
import com.solar.ev.model.quotation.QuotationListResponse
import com.solar.ev.model.quotation.QuotationRemarkRequest
import com.solar.ev.model.quotation.UpdateQuotationRequest
import com.solar.ev.model.report.AgentReportResponse // Added import
import com.solar.ev.model.suryaghar.ProjectProcessCreateData // New
import com.solar.ev.model.suryaghar.ProjectProcessCreateRequest // New
import com.solar.ev.model.suryaghar.ProjectProcessData // New
import com.solar.ev.model.suryaghar.ProjectProcessResponse // New
import com.solar.ev.model.suryaghar.ProjectProcessStatusUpdateRequest // New
import com.solar.ev.model.suryaghar.ProjectProcessUpdateRequest // New
import com.solar.ev.model.user.UserListResponse
import com.solar.ev.model.user.UpdateUserManagementRequest
import com.solar.ev.model.user.UpdateUserManagementResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("v1/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("v1/register")
    suspend fun signupUser(@Body signupRequest: SignupRequest): Response<SignupResponse>

    @GET("v1/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<ProfileResponse>

    @POST("v1/profile/update-profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<ProfileResponse>

    @POST("v1/email/verify/send")
    suspend fun sendVerificationEmail(
        @Header("Authorization") token: String
    ): Response<SendVerificationEmailResponse>

    @PUT("v1/profile/photo")
    suspend fun uploadProfilePhoto(
        @Header("Authorization") token: String,
        @Body request: UploadPhotoRequest
    ): Response<UploadPhotoResponse>

    // Application Endpoints
    @POST("v1/applications")
    suspend fun submitApplication(
        @Header("Authorization") token: String,
        @Body request: ApplicationRequest
    ): Response<ApplicationSuccessResponse>

    @GET("v1/my-applications")
    suspend fun getMyApplications(
        @Header("Authorization") token: String
    ): Response<ApplicationListResponse>

    @GET("v1/admin/applications")
    suspend fun getAdminApplications(
        @Header("Authorization") token: String
    ): Response<ApplicationListResponse>

    @GET("v1/applications/{id}")
    suspend fun getApplicationDetails(
        @Header("Authorization") token: String,
        @Path("id") applicationId: String
    ): Response<ApplicationDetailResponse>

    @PUT("v1/applications/{id}")
    suspend fun updateApplication(
        @Path("id") id: String,
        @Header("Authorization") token: String,
        @Body applicationRequest: ApplicationRequest
    ): Response<ApplicationSuccessResponse>

    @DELETE("v1/applications/{id}")
    suspend fun deleteApplication(
        @Header("Authorization") token: String,
        @Path("id") applicationId: String
    ): Response<DeleteApplicationResponse>

    @PATCH("v1/applications/{id}/apply")
    suspend fun markApplicationAsApplied(
        @Header("Authorization") token: String,
        @Path("id") applicationId: String,
        @Body request: ApplicationApplyRequest
    ): Response<ApplicationApplyResponse>

    // KYC Endpoints
    @POST("v1/kyc-documents")
    suspend fun createKYCDocument(
        @Header("Authorization") token: String,
        @Body request: KYCSubmitRequest
    ): Response<KYCSubmitResponse>

    @GET("v1/kyc-documents/{kyc-id}")
    suspend fun getKYCDocument(
        @Header("Authorization") token: String,
        @Path("kyc-id") kycId: String
    ): Response<KYCFetchResponse>

    @PUT("v1/kyc-documents/{kyc-id}")
    suspend fun updateKYCDocument(
        @Header("Authorization") token: String,
        @Path("kyc-id") kycId: String,
        @Body request: KYCSubmitRequest
    ): Response<KYCUpdateResponse>

    @DELETE("v1/kyc-documents/{id}")
    suspend fun deleteKYCDocument(
        @Header("Authorization") token: String,
        @Path("id") kycId: String
    ): Response<DetailDeleteResponse>

    @PATCH("v1/kyc-documents/{id}/verify")
    suspend fun verifyKycDocument(
        @Header("Authorization") token: String,
        @Path("id") kycId: String,
        @Body request: DocumentVerificationRequest
    ): Response<GenericVerificationResponse>

    // Bank Details Endpoints
    @POST("v1/bank-details")
    suspend fun submitBankDetails(
        @Header("Authorization") token: String,
        @Body request: BankDetailsRequest
    ): Response<BankDetailsResponse>

    @GET("v1/bank-details/{bank_id}")
    suspend fun getBankDetail(
        @Header("Authorization") token: String,
        @Path("bank_id") bankId: String
    ): Response<BankDetailFetchResponse>

    @PUT("v1/bank-details/{bank_id}")
    suspend fun updateBankDetail(
        @Header("Authorization") token: String,
        @Path("bank_id") bankId: String,
        @Body request: BankDetailsRequest
    ): Response<BankDetailsUpdateResponse>

    @DELETE("v1/bank-details/{id}")
    suspend fun deleteBankDetail(
        @Header("Authorization") token: String,
        @Path("id") bankId: String
    ): Response<DetailDeleteResponse>

    @PATCH("v1/bank-details/{id}/verify")
    suspend fun verifyBankDetail(
        @Header("Authorization") token: String,
        @Path("id") bankId: String,
        @Body request: DocumentVerificationRequest
    ): Response<GenericVerificationResponse>

    // Discom Details Endpoints
    @POST("v1/discom-details")
    suspend fun submitDiscomDetails(
        @Header("Authorization") token: String,
        @Body request: DiscomDetailsRequest
    ): Response<DiscomDetailsResponse>

    @GET("v1/discom-details/{id}")
    suspend fun getDiscomDetail(
        @Header("Authorization") token: String,
        @Path("id") discomId: String
    ): Response<DiscomDetailFetchResponse>

    @PUT("v1/discom-details/{id}")
    suspend fun updateDiscomDetail(
        @Header("Authorization") token: String,
        @Path("id") discomId: String,
        @Body request: DiscomDetailsRequest
    ): Response<DiscomDetailsResponse>

    @DELETE("v1/discom-details/{id}")
    suspend fun deleteDiscomDetail(
        @Header("Authorization") token: String,
        @Path("id") discomId: String
    ): Response<DetailDeleteResponse>

    @PATCH("v1/discom-details/{id}/verify")
    suspend fun verifyDiscomDetail(
        @Header("Authorization") token: String,
        @Path("id") discomId: String,
        @Body request: DocumentVerificationRequest
    ): Response<GenericVerificationResponse>

    // Installation Details Endpoints
    @POST("v1/installation-details")
    suspend fun submitInstallationDetails(
        @Header("Authorization") token: String,
        @Body request: InstallationDetailsRequest
    ): Response<InstallationDetailsResponse>

    @GET("v1/installation-details/{id}")
    suspend fun getInstallationDetail(
        @Header("Authorization") token: String,
        @Path("id") installationId: String
    ): Response<InstallationDetailFetchResponse>

    @PUT("v1/installation-details/{id}")
    suspend fun updateInstallationDetail(
        @Header("Authorization") token: String,
        @Path("id") installationId: String,
        @Body request: InstallationDetailsRequest
    ): Response<InstallationDetailsResponse>

    @DELETE("v1/installation-details/{id}")
    suspend fun deleteInstallationDetail(
        @Header("Authorization") token: String,
        @Path("id") installationId: String
    ): Response<DetailDeleteResponse>

    @PATCH("v1/installation-details/{id}/verify")
    suspend fun verifyInstallationDetail(
        @Header("Authorization") token: String,
        @Path("id") installationId: String,
        @Body request: DocumentVerificationRequest
    ): Response<GenericVerificationResponse>

    // User Management Endpoints (Admin)
    @GET("v1/users/grouped-by-role")
    suspend fun getUsersGroupedByRole(
        @Header("Authorization") token: String
    ): Response<UserListResponse>

    @PATCH("v1/users/{id}/status")
    suspend fun updateUserManagementDetails(
        @Header("Authorization") token: String,
        @Path("id") userId: String,
        @Body request: UpdateUserManagementRequest
    ): Response<UpdateUserManagementResponse>

    @GET("v1/dashboard/stats")
    suspend fun getDashboardStats(
        @Header("Authorization") token: String
    ): Response<DashboardStatsResponse>

    // Report Endpoints
    @GET("v1/reports/user-role-wise-status")
    suspend fun getAgentWiseReport(
        @Header("Authorization") token: String
    ): Response<AgentReportResponse>

    // Surya Ghar Project Process Endpoints
    @POST("v1/project-process")
    suspend fun createProjectProcess(
        @Header("Authorization") token: String,
        @Body request: ProjectProcessCreateRequest
    ): Response<ProjectProcessResponse<ProjectProcessCreateData>>

    @GET("v1/project-process")
    suspend fun getProjectProcesses(
        @Header("Authorization") token: String
    ): Response<ProjectProcessResponse<List<ProjectProcessData>>>

    @GET("v1/project-process/{id}") 
    suspend fun getProjectProcessById(
        @Header("Authorization") token: String,
        @Path("id") projectProcessId: String
    ): Response<ProjectProcessResponse<ProjectProcessData>>

    @PUT("v1/project-process/{id}")
    suspend fun updateProjectProcess(
        @Header("Authorization") token: String,
        @Path("id") projectProcessId: String,
        @Body request: ProjectProcessUpdateRequest
    ): Response<ProjectProcessResponse<ProjectProcessData>>

    @DELETE("v1/project-process/{id}")
    suspend fun deleteProjectProcess(
        @Header("Authorization") token: String,
        @Path("id") projectProcessId: String
    ): Response<ProjectProcessResponse<List<Any>>> // Changed from <Unit> to <List<Any>>

    @PATCH("v1/project-process/{id}/status")
    suspend fun updateProjectProcessStatus(
        @Header("Authorization") token: String,
        @Path("id") projectProcessId: String,
        @Body request: ProjectProcessStatusUpdateRequest
    ): Response<ProjectProcessResponse<ProjectProcessData>>

    // Quotation Endpoints
    @GET("v1/quotations/application/{applicationId}")
    suspend fun getQuotationsByApplication(
        @Header("Authorization") token: String,
        @Path("applicationId") applicationId: String
    ): Response<QuotationListResponse>

    @POST("v1/quotations")
    suspend fun createQuotation(
        @Header("Authorization") token: String,
        @Body request: CreateQuotationRequest
    ): Response<QuotationListResponse>

    @PUT("v1/quotations/{id}")
    suspend fun updateQuotation(
        @Header("Authorization") token: String,
        @Path("id") quotationId: String,
        @Body request: UpdateQuotationRequest
    ): Response<QuotationListResponse>

    @PATCH("v1/quotations/{id}/submit")
    suspend fun submitQuotation(
        @Header("Authorization") token: String,
        @Path("id") quotationId: String,
        @Body request: QuotationRemarkRequest
    ): Response<QuotationListResponse>

    @PATCH("v1/quotations/{id}/approve")
    suspend fun approveQuotation(
        @Header("Authorization") token: String,
        @Path("id") quotationId: String,
        @Body request: QuotationRemarkRequest
    ): Response<QuotationListResponse>

    @PATCH("v1/quotations/{id}/reject")
    suspend fun rejectQuotation(
        @Header("Authorization") token: String,
        @Path("id") quotationId: String,
        @Body request: QuotationRemarkRequest
    ): Response<QuotationListResponse>

    @PATCH("v1/quotations/{id}/request-revision")
    suspend fun requestRevisionQuotation(
        @Header("Authorization") token: String,
        @Path("id") quotationId: String,
        @Body request: QuotationRemarkRequest
    ): Response<QuotationListResponse>

    @DELETE("v1/quotations/{id}")
    suspend fun deleteQuotation(
        @Header("Authorization") token: String,
        @Path("id") quotationId: String
    ): Response<QuotationListResponse>
}
