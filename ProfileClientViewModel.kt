package com.md.matur.feature.profile.client

import com.md.matur.NailsApplication
import com.md.matur.data.utils.AppDatabase
import com.md.matur.data.repository.NailsApiService
import com.md.matur.feature.base.mvvm.BaseViewModel
import com.md.matur.data.repository.usecases.user.UpdateUserUseCase
import com.md.matur.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import okhttp3.MultipartBody
import javax.inject.Inject


class ProfileClientViewModel : BaseViewModel() {

    @Inject
    lateinit var nailsApiService: NailsApiService

    @Inject
    lateinit var config: Config


    @Inject
    lateinit var updateUserUseCase: UpdateUserUseCase

    init {
        NailsApplication.loggedUserComponent.inject(this)
    }

    fun onLogout() {
        viewModelScope.launch(Dispatchers.IO) {
            db.clearAllTables()
        }
    }

    fun loadProfile() {
        val disposable = nailsApiService.getUserProfile(config.token)
            .setupThreads()
            .subscribeBy(
                onSuccess = {
                    config.currentProfile = it
                },
                onError = {
                    newToast = handleNetExceptions(it)
                }
            )
        compositeDisposable.add(disposable)
    }

    fun uploadProfilePhoto(requestBody: MultipartBody.Part) {
        compositeDisposable.add(
            updateUserUseCase.uploadProfilePhoto(config.token, requestBody).subscribeBy(
                onSuccess = {
                    loadProfile()
                },
                onError = {
                    newToast = handleNetExceptions(it)
                }
            )
        )
    }

    fun deletePhoto() {
        val disposable =
            nailsApiService.deletePhoto(config.token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = {
                        config.currentProfile = config.currentProfile?.copy(link_photo = null)
                        newToast = "Фото профиля успешно удалено"
                    },
                    onError = {
                        newToast = handleNetExceptions(it)
                    }
                )
        compositeDisposable.add(disposable)
    }

}