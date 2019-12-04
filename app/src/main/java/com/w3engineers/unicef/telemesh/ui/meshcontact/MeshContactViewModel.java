package com.w3engineers.unicef.telemesh.ui.meshcontact;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.w3engineers.ext.strom.application.ui.base.BaseRxAndroidViewModel;
import com.w3engineers.unicef.telemesh.data.helper.TeleMeshDataHelper;
import com.w3engineers.unicef.telemesh.data.local.usertable.UserDataSource;
import com.w3engineers.unicef.telemesh.data.local.usertable.UserEntity;
import com.w3engineers.unicef.telemesh.data.pager.MainThreadExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


/*
 * ============================================================================
 * Copyright (C) 2019 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * ============================================================================
 */
public class MeshContactViewModel extends BaseRxAndroidViewModel {

    private UserDataSource userDataSource;
    private MutableLiveData<UserEntity> openUserMessage = new MutableLiveData<>();
    MutableLiveData<PagedList<UserEntity>> allMessagedWithEntity = new MutableLiveData<>();
    MutableLiveData<PagedList<UserEntity>> favoriteEntityList = new MutableLiveData<>();
    MutableLiveData<List<UserEntity>> backUserEntity = new MutableLiveData<>();
    private MutableLiveData<PagedList<UserEntity>> filterUserList = new MutableLiveData<>();

    private static final int INITIAL_LOAD_KEY = 0;
    private static final int PAGE_SIZE = 50;
    private static final int PREFETCH_DISTANCE = 30;

    private List<UserEntity> userList;

    private String searchableText;


    public MeshContactViewModel(@NonNull Application application) {
        super(application);
        userDataSource = UserDataSource.getInstance();
    }


    public void openMessage(@NonNull UserEntity userEntity) {
        openUserMessage.postValue(userEntity);
    }

    public void setSearchText(String searchText) {
        this.searchableText = searchText;
    }

    public int getUserAvatarByIndex(int imageIndex) {
        return TeleMeshDataHelper.getInstance().getAvatarImage(imageIndex);
    }

    MutableLiveData<UserEntity> openUserMessage() {
        return openUserMessage;
    }

    public void startAllMessagedWithFavouriteObserver() {
        getCompositeDisposable().add(userDataSource.getAllMessagedWithFavouriteUsers()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userEntities -> {

                    if (!TextUtils.isEmpty(searchableText)) {
                        backUserEntity.postValue(userEntities);
                        startSearch(searchableText, userEntities);
                    } else {

                        UserPositionalDataSource userSearchDataSource = new UserPositionalDataSource(userEntities);

                        PagedList.Config myConfig = new PagedList.Config.Builder()
                                .setEnablePlaceholders(true)
                                .setPrefetchDistance(PREFETCH_DISTANCE)
                                .setPageSize(PAGE_SIZE)
                                .build();


                        PagedList<UserEntity> pagedStrings = new PagedList.Builder<>(userSearchDataSource, myConfig)
                                .setInitialKey(INITIAL_LOAD_KEY)
                                .setNotifyExecutor(new MainThreadExecutor()) //The executor defining where page loading updates are dispatched.asset
                                .setFetchExecutor(Executors.newSingleThreadExecutor())
                                .build();

                        allMessagedWithEntity.postValue(pagedStrings);
                    }

                }, throwable -> {
                    throwable.printStackTrace();
                }));

        //  allMessagedWithEntity = userDataSource.getAllMessagedWithFavouriteUsers();
    }

    public void startFavouriteObserver() {

        getCompositeDisposable().add(userDataSource.getFavouriteUsers()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userEntities -> {

                    userList = userEntities;

                    if (!TextUtils.isEmpty(searchableText)) {
                        backUserEntity.postValue(userEntities);
                        startSearch(searchableText, userEntities);
                    } else {

                        UserPositionalDataSource userSearchDataSource = new UserPositionalDataSource(userEntities);

                        PagedList.Config myConfig = new PagedList.Config.Builder()
                                .setEnablePlaceholders(true)
                                .setPrefetchDistance(PREFETCH_DISTANCE)
                                .setPageSize(PAGE_SIZE)
                                .build();


                        PagedList<UserEntity> pagedStrings = new PagedList.Builder<>(userSearchDataSource, myConfig)
                                .setInitialKey(INITIAL_LOAD_KEY)
                                .setNotifyExecutor(new MainThreadExecutor()) //The executor defining where page loading updates are dispatched.asset
                                .setFetchExecutor(Executors.newSingleThreadExecutor())
                                .build();

                        favoriteEntityList.postValue(pagedStrings);
                    }

                }, throwable -> {
                    throwable.printStackTrace();
                }));

        //  favoriteEntityList = userDataSource.getFavouriteUsers();

    }

    public List<UserEntity> getCurrentUserList() {
        if (userList == null) {
            userList = new ArrayList<>();
        }
        return userList;
    }


    @NonNull
    public LiveData<PagedList<UserEntity>> getGetFilteredList() {
        return filterUserList;
    }


    public void startSearch(@NonNull String searchText, @Nullable List<UserEntity> userEntities) {

        searchableText = searchText;

        if (userEntities != null) {
            List<UserEntity> filteredItemList = new ArrayList<>();

            for (UserEntity user : userEntities) {

                if (user.getFullName().toLowerCase(Locale.getDefault()).contains(searchText))
                    filteredItemList.add(user);
            }
            Log.d("SearchIssue", "user list post call");

            UserPositionalDataSource userSearchDataSource = new UserPositionalDataSource(filteredItemList);

            PagedList.Config myConfig = new PagedList.Config.Builder()
                    .setEnablePlaceholders(true)
                    .setPrefetchDistance(PREFETCH_DISTANCE)
                    .setPageSize(PAGE_SIZE)
                    .build();


            PagedList<UserEntity> pagedStrings = new PagedList.Builder<>(userSearchDataSource, myConfig)
                    .setInitialKey(INITIAL_LOAD_KEY)
                    .setNotifyExecutor(new MainThreadExecutor()) //The executor defining where page loading updates are dispatched.
                    .setFetchExecutor(Executors.newSingleThreadExecutor())
                    .build();


            filterUserList.postValue(pagedStrings);

        } else {
            Log.d("SearchIssue", "user list null");
        }
    }

}
