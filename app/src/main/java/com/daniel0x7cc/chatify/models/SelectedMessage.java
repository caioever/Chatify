package com.daniel0x7cc.chatify.models;

import com.sendbird.android.FileMessage;
import com.sendbird.android.UserMessage;

import java.io.Serializable;

public class SelectedMessage implements Serializable {

    private Class objClass;
    private String message;
    private String fileUrl;
    private String fileName;
    private int fileSize;
    private String fileType;
    private String customType;

    public SelectedMessage(FileMessage message) {
        this.objClass = message.getClass();
        this.message = null;
        this.fileUrl = message.getUrl();
        this.fileName = message.getName();
        this.fileSize = message.getSize();
        this.fileType = message.getType();
        this.customType = message.getCustomType();
    }

    public SelectedMessage(UserMessage message) {
        this.objClass = message.getClass();
        this.message = message.getMessage();
        this.fileUrl = null;
        this.fileName = null;
        this.fileSize = 0;
        this.fileType = null;
    }

    public Class getObjClass() {
        return objClass;
    }

    public void setObjClass(Class objClass) {
        this.objClass = objClass;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getCustomType(){
        return customType;
    }

}
