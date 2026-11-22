package com.kien.movies.data.remote.api

import com.kien.movies.data.remote.dto.AuthDiagnosticReportRequestDto
import com.kien.movies.data.remote.dto.AuthDiagnosticReportResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthDiagnosticReportApi {
    @POST("api/auth-diagnostics")
    suspend fun createAuthDiagnosticReport(
        @Body body: AuthDiagnosticReportRequestDto
    ): Response<AuthDiagnosticReportResponseDto>
}
