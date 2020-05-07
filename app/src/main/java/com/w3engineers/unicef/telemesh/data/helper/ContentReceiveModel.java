package com.w3engineers.unicef.telemesh.data.helper;

public class ContentReceiveModel {
    private String contentId, contentPath, userId;
    private int contentReceiveProgress;
    private boolean successStatus;
    private ContentMessageModel contentMessageModel;

    public String getContentId() {
        return contentId;
    }

    public ContentReceiveModel setContentId(String contentId) {
        this.contentId = contentId;
        return this;
    }

    public String getContentPath() {
        return contentPath;
    }

    public ContentReceiveModel setContentPath(String contentPath) {
        this.contentPath = contentPath;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public ContentReceiveModel setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public int getContentReceiveProgress() {
        return contentReceiveProgress;
    }

    public ContentReceiveModel setContentReceiveProgress(int contentReceiveProgress) {
        this.contentReceiveProgress = contentReceiveProgress;
        return this;
    }

    public boolean isSuccessStatus() {
        return successStatus;
    }

    public ContentReceiveModel setSuccessStatus(boolean successStatus) {
        this.successStatus = successStatus;
        return this;
    }

    public ContentMessageModel getContentMessageModel() {
        return contentMessageModel;
    }

    public ContentReceiveModel setContentMessageModel(ContentMessageModel contentMessageModel) {
        this.contentMessageModel = contentMessageModel;
        return this;
    }
}
