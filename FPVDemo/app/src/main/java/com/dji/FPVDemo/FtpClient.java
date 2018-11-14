package com.dji.FPVDemo;

/**
 * Created by Lenovo on 2018/11/14.
 */
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;




import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import dji.thirdparty.sanselan.util.IOUtils;


public class FtpClient {

    /**
     * 传输文件到ftp服务器
     * @param file
     * @return
     */
    private boolean uploadFile(File file) {
        FTPClient ftpClient = new FTPClient();
        if(file == null || !file.exists()){
            log.error("文件不存在! file="+file);
            return false;
        }

        CountingInputStream cis = null;
        try{
            ftpClient.connect(host, port);
            ftpClient.login(userName, password);
            ftpClient.enterLocalPassiveMode();   // 进入被动模式
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE); // 需要指定文件传输类型，否则默认是ASCII类型，会导致二进制文件传输损坏
            InputStream ins = new FileInputStream(file);

            // 提示文件上传进度
            final long fileSize = file.length();
            cis = new CountingInputStream(ins){
                private double progress = 0.0;
                private double step     = 0.1;
                @Override
                protected void beforeRead(int n){
                    try {
                        super.beforeRead(n);
                    } catch (IOException e) {
                        logger.error("error reading file.", e);
                    }
                    double ratio = 1.0 * getCount() / fileSize;
                    if(ratio >= progress){
                        logger.info(String.format("uploading %s of %s (%.0f%% completed)", getCount(), fileSize, ratio * 100));
                        progress += step;
                    }
                }
            };
            return ftpClient.storeFile(file.getName(), cis);
        }catch (Exception e) {
            logger.error(String.format("ftp文件传输异常, file=%s", file), e);
            return false;
        }finally{

            IOUtils.closeQuietly(cis);
            try { ftpClient.logout(); } catch (Exception e) {} // keep quiet
            try { ftpClient.disconnect(); } catch (IOException e) {}// keep quiet
        }
    }
}

