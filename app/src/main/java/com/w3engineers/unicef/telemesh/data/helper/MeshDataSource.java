package com.w3engineers.unicef.telemesh.data.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.w3engineers.ext.strom.util.helper.data.local.SharedPref;
import com.w3engineers.mesh.util.lib.mesh.HandlerUtil;
import com.w3engineers.unicef.TeleMeshApplication;
import com.w3engineers.unicef.telemesh.data.broadcast.BroadcastManager;
import com.w3engineers.unicef.telemesh.data.broadcast.SendDataTask;
import com.w3engineers.unicef.telemesh.data.helper.constants.Constants;
import com.w3engineers.unicef.telemesh.data.local.usertable.UserModel;
import com.w3engineers.unicef.util.helper.BulletinTimeScheduler;
import com.w3engineers.unicef.util.helper.TextToImageHelper;
import com.w3engineers.unicef.util.helper.ViperUtil;
import com.w3engineers.unicef.util.helper.model.ViperContentData;
import com.w3engineers.unicef.util.helper.model.ViperData;

import java.util.HashMap;
import java.util.List;

/*
 * ============================================================================
 * Copyright (C) 2019 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * ============================================================================
 */
public class MeshDataSource extends ViperUtil {

    @SuppressLint("StaticFieldLeak")
    private static MeshDataSource rightMeshDataSource;
    public static boolean isPrepared = false;
    private BroadcastManager broadcastManager;

    private HashMap<String, ContentReceiveModel> contentReceiveModelHashMap = new HashMap<>();

    private MeshDataSource(@NonNull UserModel userModel) {
        super(userModel);
        broadcastManager = BroadcastManager.getInstance();
    }


    @NonNull
    static MeshDataSource getRmDataSource() {
        if (rightMeshDataSource == null) {
            Context context = TeleMeshApplication.getContext();

            SharedPref sharedPref = SharedPref.getSharedPref(context);

            UserModel userModel = new UserModel()
                    .setName(sharedPref.read(Constants.preferenceKey.USER_NAME))
                    .setImage(sharedPref.readInt(Constants.preferenceKey.IMAGE_INDEX))
                    .setTime(sharedPref.readLong(Constants.preferenceKey.MY_REGISTRATION_TIME));

            rightMeshDataSource = new MeshDataSource(userModel);
        }
        return rightMeshDataSource;
    }

    @Override
    protected void onMesh(String myMeshId) {
        meshInited(myMeshId);
        RmDataHelper.getInstance().meshInitiated();
    }

    @Override
    protected void onMeshPrepared(String myWalletAddress) {
        meshInited(myWalletAddress);
    }

    private void meshInited(String meshId) {
        //when RM will be on then prepare this observer to listen the outgoing messages

        SharedPref.getSharedPref(TeleMeshApplication.getContext()).write(Constants.preferenceKey.MY_USER_ID, meshId);

        if (!isPrepared) {
            RmDataHelper.getInstance().prepareDataObserver();
            TextToImageHelper.writeWalletAddressToImage(meshId);
            isPrepared = true;
        }

        Constants.IsMeshInit = true;

        BulletinTimeScheduler.getInstance().checkAppUpdate();
    }

    /*public void stopAllServices() {
        // TODO stop service during mode change from data plan mode
    }*/

    /**
     * During send data to peer
     *
     * @param dataModel -> A generic data model which contains userData, type and peerId
     * @return return the send message id
     */
    public void DataSend(@NonNull DataModel dataModel, @NonNull String receiverId, boolean isNotificationEnable) {

        if (!TextUtils.isEmpty(receiverId)) {
            dataModel.setUserId(receiverId);

            ViperData viperData = new ViperData();
            viperData.rawData = dataModel.getRawData();
            viperData.dataType = dataModel.getDataType();
            viperData.isNotificationEnable = isNotificationEnable;

            broadcastManager.addBroadCastMessage(getMeshDataTask(viperData, receiverId));
        }
    }

    public void DataSend(@NonNull DataModel dataModel, @NonNull List<String> receiverIds, boolean isNotificationEnable) {
        for (String receiverId : receiverIds) {
            DataSend(dataModel, receiverId, isNotificationEnable);
        }
    }

    private SendDataTask getMeshDataTask(ViperData viperData, String receiverId) {
        return new SendDataTask().setPeerId(receiverId).setMeshData(viperData).setBaseRmDataSource(this);
    }

    public void ContentDataSend(ContentModel contentModel, boolean notificationEnable) {
        String receiverId = contentModel.getUserId();

        if (!TextUtils.isEmpty(receiverId)) {
            ViperContentData viperContentData = new ViperContentData();
            viperContentData.dataType = contentModel.getContentDataType();
            viperContentData.contentModel = contentModel;
            viperContentData.isNotificationEnable = notificationEnable;

            broadcastManager.addBroadCastMessage(getMeshContentDataTask(receiverId, viperContentData));
        }
    }

    private SendDataTask getMeshContentDataTask(String receiverId, ViperContentData viperContentData) {
        return new SendDataTask().setPeerId(receiverId).setViperContentData(viperContentData)
                .setBaseRmDataSource(this);
    }

    /**
     * During receive a peer this time onPeer api is execute
     *
     * @param peerData -> Got a peer data (profile information)
     */
    protected void peerAdd(String peerId, byte[] peerData) {

        try {

            if (!TextUtils.isEmpty(peerId)) {
                String userString = new String(peerData);
                UserModel userModel = new Gson().fromJson(userString, UserModel.class);

                if (userModel != null) {
                    userModel.setUserId(peerId);
                    HandlerUtil.postBackground(() -> RmDataHelper.getInstance().userAdd(userModel));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void peerAdd(String peerId, UserModel userModel) {

        if (!TextUtils.isEmpty(peerId)) {
            if (userModel != null) {
                userModel.setUserId(peerId);
                HandlerUtil.postBackground(() -> RmDataHelper.getInstance().userAdd(userModel));
            }
        }
    }

    /**
     * When a peer is gone or switched the another network
     * this time onPeerGone api is executed
     *
     * @param peerId - > It contains the peer id which is currently inactive in mesh
     */
    @Override
    protected void peerRemove(@NonNull String peerId) {

        if (!TextUtils.isEmpty(peerId)) {
            HandlerUtil.postBackground(() -> RmDataHelper.getInstance().userLeave(peerId));
        }
    }

    /**
     * This api execute during we receive data from network
     *
     * @param viperData -> Contains data and peer info also
     */
    @Override
    protected void onData(@NonNull String peerId, ViperData viperData) {

        if (!TextUtils.isEmpty(peerId)) {
            DataModel dataModel = new DataModel()
                    .setUserId(peerId)
                    .setRawData(viperData.rawData)
                    .setDataType(viperData.dataType);

            HandlerUtil.postBackground(() -> RmDataHelper.getInstance().dataReceive(dataModel, true));
        }
    }

    /**
     * The sending data status is success this time we got a success ack using this api
     *
     * @param messageId -> Contains the success data id and user id
     */
    @Override
    protected void onAck(@NonNull String messageId, int status) {
        DataModel rmDataModel = new DataModel()
                .setDataTransferId(messageId)
                .setAckSuccess(true);

        HandlerUtil.postBackground(() -> RmDataHelper.getInstance().ackReceive(rmDataModel, status));
    }

    @Override
    protected boolean isNodeAvailable(String nodeId, int userActiveStatus) {
        return RmDataHelper.getInstance().userExistedOperation(nodeId, userActiveStatus);
    }

    @Override
    protected void contentReceiveStart(String contentId, String contentPath, String userId, byte[] metaData) {

        ContentReceiveModel contentReceiveModel = contentReceiveModelHashMap.get(contentId);

        if (contentReceiveModel != null) {
            contentReceiveModel.setContentPath(contentPath)
                    .setUserId(userId).setMetaData(metaData);
        } else {
            contentReceiveModel = new ContentReceiveModel()
                    .setContentId(contentId)
                    .setContentPath(contentPath)
                    .setUserId(userId)
                    .setMetaData(metaData)
                    .setSuccessStatus(true);
        }
        contentReceiveModelHashMap.put(contentId, contentReceiveModel);
    }

    @Override
    protected void contentReceiveInProgress(String contentId, int progress) {
        ContentReceiveModel contentReceiveModel = contentReceiveModelHashMap.get(contentId);
        if (contentReceiveModel != null) {
            contentReceiveModel.setContentReceiveProgress(progress);
            contentReceiveModelHashMap.put(contentId, contentReceiveModel);
        }
    }

    @Override
    protected void contentReceiveDone(String contentId, boolean contentStatus) {
        ContentReceiveModel contentReceiveModel = contentReceiveModelHashMap.get(contentId);
        if (contentReceiveModel != null) {
            if (contentStatus) {

                String contentMessageText = new String(contentReceiveModel.getMetaData());
                ContentMessageModel contentMessageModel = new Gson().fromJson(contentMessageText,
                        ContentMessageModel.class);

                String contentPath = null, thumbPath = null;

                switch (contentMessageModel.getContentType()) {
                    case Constants.DataType.CONTENT_MESSAGE:
                        contentPath = contentReceiveModel.getContentPath();
                        break;
                    case Constants.DataType.CONTENT_THUMB_MESSAGE:
                        thumbPath = contentReceiveModel.getContentPath();
                        break;
                }

                ContentModel contentModel = new ContentModel()
                        .setMessageId(contentMessageModel.getMessageId())
                        .setMessageType(contentMessageModel.getMessageType())
                        .setContentPath(contentPath)
                        .setThumbPath(thumbPath)
                        .setContentDataType(contentMessageModel.getContentType())
                        .setUserId(contentReceiveModel.getUserId());

                HandlerUtil.postBackground(() -> RmDataHelper.getInstance()
                        .contentReceive(contentModel, true));
                // Finished
            } else {
                // Failed
            }
        }
        contentReceiveModelHashMap.remove(contentId);
    }

    /*@Override
    protected void configSync(boolean isUpdate, ConfigurationCommand configurationCommand) {
        RmDataHelper.getInstance().syncConfigFileAndBroadcast(isUpdate, configurationCommand);
    }*/

    // TODO SSID_Change
    /*public void resetInstance() {
        rightMeshDataSource = null;
    }*/

    public void saveUpdateUserInfo() {

        Context context = TeleMeshApplication.getContext();

        SharedPref sharedPref = SharedPref.getSharedPref(context);

        UserModel userModel = new UserModel()
                .setName(sharedPref.read(Constants.preferenceKey.USER_NAME))
                .setImage(sharedPref.readInt(Constants.preferenceKey.IMAGE_INDEX))
                .setTime(sharedPref.readLong(Constants.preferenceKey.MY_REGISTRATION_TIME));

        saveUserInfo(userModel);

    }

    public void saveUpdateOtherUserInfo(String userAddress, String userName, int imageIndex) {
        UserModel userModel = new UserModel().setUserId(userAddress)
                .setName(userName).setImage(imageIndex);

        saveOtherUserInfo(userModel);
    }

    public void checkUserIsConnected(String userId) {
        checkUserConnectionStatus(userId);
    }

}
