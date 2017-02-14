package com.google.photoselector.bean;

import android.text.TextUtils;

import java.io.Serializable;

/**
 * ============================================================
 * Copyright：${TODO}有限公司版权所有 (c) 2016
 * Author：   陈冠杰
 * Email：    815712739@qq.com
 * GitHub：   https://github.com/JackChen1999
 * <p/>
 * Project_Name：PhotoSelector
 * Package_Name：com.google.photoselector.bean
 * Version：1.0
 * time：2016/8/4 21:16
 * des ：${TODO}
 * gitVersion：$Rev$
 * updateAuthor：$Author$
 * updateDate：$Date$
 * updateDes：${TODO}
 * ============================================================
 **/
public class FolderBean implements Serializable{
    private static final long serialVersionUID = 1L;

    private String dirPath;       // 文件夹路径
    private String firstImgPath;  // 第一张图片路径
    private String dirName;       // 文件夹名字
    private int imgCount;         // 当前文件夹中图片数量
    private boolean isSel;        // 当前文件是否选中

    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;

        if (TextUtils.isEmpty(dirName)) {
            int lastIndexOf = dirPath.lastIndexOf("/") + 1;
            setDirName(dirPath.substring(lastIndexOf));
        }
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }

    public String getDirName() {
        return dirName;
    }

    public void setDirName(String dirName) {
        this.dirName = dirName;
    }

    /**
     * 返回图片数目：2
     *
     * @return int
     */
    public int getImgCount() {
        return imgCount;
    }

    /**
     * 返回图片数目：2张
     *
     * @return String
     */
    public String getImgCountStr() {
        return imgCount + "张";
    }

    public void setImgCount(int imgCount) {
        this.imgCount = imgCount;
    }

    public boolean isSel() {
        return isSel;
    }

    public void setSel(boolean isSel) {
        this.isSel = isSel;
    }
}
