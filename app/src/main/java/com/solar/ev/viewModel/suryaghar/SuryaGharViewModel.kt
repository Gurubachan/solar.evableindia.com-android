package com.solar.ev.viewModel.suryaghar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solar.ev.model.application.ApplicationDetailResponse
import com.solar.ev.model.quotation.CreateQuotationRequest
import com.solar.ev.model.quotation.QuotationListResponse
import com.solar.ev.model.quotation.QuotationRemarkRequest
import com.solar.ev.model.quotation.UpdateQuotationRequest
import com.solar.ev.model.suryaghar.*
import com.solar.ev.network.ApiService
import com.solar.ev.util.NetworkUtils // Changed import from com.solar.ev.network.NetworkUtils
import kotlinx.coroutines.launch

class SuryaGharViewModel(private val apiService: ApiService) : ViewModel() {

    // Create Project Process
    private val _createResult = MutableLiveData<SuryaGharApiResult<ProjectProcessResponse<ProjectProcessCreateData>>>()
    val createResult: LiveData<SuryaGharApiResult<ProjectProcessResponse<ProjectProcessCreateData>>> = _createResult

    fun createProjectProcess(token: String, request: ProjectProcessCreateRequest) {
        _createResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.createProjectProcess("Bearer $token", request)
                if (response.isSuccessful && response.body() != null) {
                    _createResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _createResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred", 
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _createResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // Get Project Processes (List)
    private val _listResult = MutableLiveData<SuryaGharApiResult<ProjectProcessResponse<List<ProjectProcessData>>>>()
    val listResult: LiveData<SuryaGharApiResult<ProjectProcessResponse<List<ProjectProcessData>>>> = _listResult

    fun getProjectProcesses(token: String) {
        _listResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.getProjectProcesses("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _listResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _listResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred", 
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _listResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // Get Project Process by ID
    private val _detailResult = MutableLiveData<SuryaGharApiResult<ProjectProcessResponse<ProjectProcessData>>>()
    val detailResult: LiveData<SuryaGharApiResult<ProjectProcessResponse<ProjectProcessData>>> = _detailResult

    fun getProjectProcessById(token: String, id: String) {
        _detailResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.getProjectProcessById("Bearer $token", id)
                if (response.isSuccessful && response.body() != null) {
                    _detailResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _detailResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred", 
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _detailResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // Update Project Process
    private val _updateResult = MutableLiveData<SuryaGharApiResult<ProjectProcessResponse<ProjectProcessData>>>()
    val updateResult: LiveData<SuryaGharApiResult<ProjectProcessResponse<ProjectProcessData>>> = _updateResult

    fun updateProjectProcess(token: String, id: String, request: ProjectProcessUpdateRequest) {
        _updateResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.updateProjectProcess("Bearer $token", id, request)
                if (response.isSuccessful && response.body() != null) {
                    _updateResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _updateResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred", 
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _updateResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // Delete Project Process
    private val _deleteResult = MutableLiveData<SuryaGharApiResult<ProjectProcessResponse<List<Any>>>>() // Changed Unit to List<Any>
    val deleteResult: LiveData<SuryaGharApiResult<ProjectProcessResponse<List<Any>>>> = _deleteResult // Changed Unit to List<Any>

    fun deleteProjectProcess(token: String, id: String) {
        _deleteResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.deleteProjectProcess("Bearer $token", id)
                if (response.isSuccessful && response.body() != null) {
                    _deleteResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _deleteResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred", 
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _deleteResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // Update Project Process Status
    private val _statusUpdateResult = MutableLiveData<SuryaGharApiResult<ProjectProcessResponse<ProjectProcessData>>>()
    val statusUpdateResult: LiveData<SuryaGharApiResult<ProjectProcessResponse<ProjectProcessData>>> = _statusUpdateResult

    fun updateProjectProcessStatus(token: String, id: String, request: ProjectProcessStatusUpdateRequest) {
        _statusUpdateResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.updateProjectProcessStatus("Bearer $token", id, request)
                if (response.isSuccessful && response.body() != null) {
                    _statusUpdateResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _statusUpdateResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred", 
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _statusUpdateResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // Get Application Details
    private val _applicationDetailResult = MutableLiveData<SuryaGharApiResult<ApplicationDetailResponse>>()
    val applicationDetailResult: LiveData<SuryaGharApiResult<ApplicationDetailResponse>> = _applicationDetailResult

    fun getApplicationDetails(token: String, id: String) {
        _applicationDetailResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.getApplicationDetails("Bearer $token", id)
                if (response.isSuccessful && response.body() != null) {
                    _applicationDetailResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _applicationDetailResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred",
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _applicationDetailResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // Get Quotations by Application
    private val _quotationListResult = MutableLiveData<SuryaGharApiResult<QuotationListResponse>>()
    val quotationListResult: LiveData<SuryaGharApiResult<QuotationListResponse>> = _quotationListResult

    fun getQuotationsByApplication(token: String, applicationId: String) {
        _quotationListResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.getQuotationsByApplication("Bearer $token", applicationId)
                if (response.isSuccessful && response.body() != null) {
                    _quotationListResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _quotationListResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred",
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _quotationListResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // Create Quotation
    private val _createQuotationResult = MutableLiveData<SuryaGharApiResult<QuotationListResponse>>()
    val createQuotationResult: LiveData<SuryaGharApiResult<QuotationListResponse>> = _createQuotationResult

    fun createQuotation(token: String, request: CreateQuotationRequest) {
        _createQuotationResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.createQuotation("Bearer $token", request)
                if (response.isSuccessful && response.body() != null) {
                    _createQuotationResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _createQuotationResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred",
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _createQuotationResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // Delete Quotation
    private val _deleteQuotationResult = MutableLiveData<SuryaGharApiResult<QuotationListResponse>>()
    val deleteQuotationResult: LiveData<SuryaGharApiResult<QuotationListResponse>> = _deleteQuotationResult

    fun deleteQuotation(token: String, quotationId: String) {
        _deleteQuotationResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.deleteQuotation("Bearer $token", quotationId)
                if (response.isSuccessful && response.body() != null) {
                    _deleteQuotationResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _deleteQuotationResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred",
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _deleteQuotationResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
    
    // Update Quotation
    private val _updateQuotationResult = MutableLiveData<SuryaGharApiResult<QuotationListResponse>>()
    val updateQuotationResult: LiveData<SuryaGharApiResult<QuotationListResponse>> = _updateQuotationResult

    fun updateQuotation(token: String, quotationId: String, request: UpdateQuotationRequest) {
        _updateQuotationResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.updateQuotation("Bearer $token", quotationId, request)
                if (response.isSuccessful && response.body() != null) {
                    _updateQuotationResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _updateQuotationResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred",
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _updateQuotationResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // Submit Quotation
    private val _submitQuotationResult = MutableLiveData<SuryaGharApiResult<QuotationListResponse>>()
    val submitQuotationResult: LiveData<SuryaGharApiResult<QuotationListResponse>> = _submitQuotationResult

    fun submitQuotation(token: String, quotationId: String, request: QuotationRemarkRequest) {
        _submitQuotationResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.submitQuotation("Bearer $token", quotationId, request)
                if (response.isSuccessful && response.body() != null) {
                    _submitQuotationResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _submitQuotationResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred",
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _submitQuotationResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // Approve Quotation
    private val _approveQuotationResult = MutableLiveData<SuryaGharApiResult<QuotationListResponse>>()
    val approveQuotationResult: LiveData<SuryaGharApiResult<QuotationListResponse>> = _approveQuotationResult

    fun approveQuotation(token: String, quotationId: String, request: QuotationRemarkRequest) {
        _approveQuotationResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.approveQuotation("Bearer $token", quotationId, request)
                if (response.isSuccessful && response.body() != null) {
                    _approveQuotationResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _approveQuotationResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred",
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _approveQuotationResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // Reject Quotation
    private val _rejectQuotationResult = MutableLiveData<SuryaGharApiResult<QuotationListResponse>>()
    val rejectQuotationResult: LiveData<SuryaGharApiResult<QuotationListResponse>> = _rejectQuotationResult

    fun rejectQuotation(token: String, quotationId: String, request: QuotationRemarkRequest) {
        _rejectQuotationResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.rejectQuotation("Bearer $token", quotationId, request)
                if (response.isSuccessful && response.body() != null) {
                    _rejectQuotationResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _rejectQuotationResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred",
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _rejectQuotationResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    // Request Revision Quotation
    private val _requestRevisionQuotationResult = MutableLiveData<SuryaGharApiResult<QuotationListResponse>>()
    val requestRevisionQuotationResult: LiveData<SuryaGharApiResult<QuotationListResponse>> = _requestRevisionQuotationResult

    fun requestRevisionQuotation(token: String, quotationId: String, request: QuotationRemarkRequest) {
        _requestRevisionQuotationResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.requestRevisionQuotation("Bearer $token", quotationId, request)
                if (response.isSuccessful && response.body() != null) {
                    _requestRevisionQuotationResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response)
                    _requestRevisionQuotationResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred",
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _requestRevisionQuotationResult.value = SuryaGharApiResult.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}
