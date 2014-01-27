//========================  FTPUpLoader.java  ==================================
package com.prelaunch.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import android.os.Handler;
import android.os.Message;

/**
* ftp서버 접속하여 파일을 업로드/다운로드 함
* @author 조한중(hanjoongcho@gmail.com)
*
*/
public class FTPUpLoader {
  /**
   * 
   * @param ip         ftp서버ip
   * @param port       ftp서버port
   * @param id         사용자id
   * @param password   사용자password 
   * @param folder     ftp서버에생성할폴더
   * @param files      업로드list
   * @return
   */
  String t;
  public boolean sendFtpServer(String ip, int port, String id, String password,
                        String folder,String localPath, ArrayList<String> files) {
      boolean isSuccess = false;
      FTPClient ftp = null;
      int reply;
      Handler ftpHdr = Main.handler;
      try {
          ftp = new FTPClient();
          ftp.connect(ip, port);
		  Message msg = ftpHdr.obtainMessage();
          msg.obj = "\n\nConnected to " + ip + " on "+ftp.getRemotePort()+"\n";
          msg.arg1 = 2;
          ftpHdr.sendMessage(msg);
          
          //MainActivity.printApp.append("\nConnected to " + ip + " on "+ftp.getRemotePort());
          
          // After connection attempt, you should check the reply code to verify
          // success.
          reply = ftp.getReplyCode();
          if (!FTPReply.isPositiveCompletion(reply)) {
              ftp.disconnect();
              System.err.println("FTP server refused connection.");
              System.exit(1);
          }
          
          if(!ftp.login(id, password)) {
              ftp.logout();
              throw new Exception("ftp 서버에 로그인하지 못했습니다.");
          }
          
          ftp.setFileType(FTP.BINARY_FILE_TYPE);
          ftp.enterLocalPassiveMode();
          

          System.out.println(ftp.printWorkingDirectory());
          try{
              ftp.makeDirectory(folder);
          }catch(Exception e){
              e.printStackTrace();
          }
          ftp.changeWorkingDirectory(folder);
          System.out.println(ftp.printWorkingDirectory());
          
          
          for(int i = 0; i < files.size(); i++) {
              //ftp서버에 한글파일을 쓸때 한글깨짐 방지
              String tempFileName = new String(files.get(i).getBytes("utf-8"),"iso_8859_1");
              String sourceFile = localPath + files.get(i);        
              File uploadFile = new File(sourceFile);
              FileInputStream fis = null;
              try {
                  fis = new FileInputStream(uploadFile);
				  msg = ftpHdr.obtainMessage();
		          msg.obj = "\n"+ tempFileName + " : 전송시작 => ";
		          msg.arg1 = 2;
		          ftpHdr.sendMessage(msg);
		          
		          // Get date
		          long now = System.currentTimeMillis();
		          SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);
		          Date date = new Date(now);
		          String strNow = sdf.format(date);
		          
		          // Rename
		          //String newName = tempFileName.substring(0,8)+"_"+strNow.substring(4,8)+strNow.substring(9,13)+".txt";
		          //uploadFile.renameTo(new File(localPath+newName));

		          //File send
		          isSuccess = ftp.storeFile(tempFileName, fis);
		          msg = ftpHdr.obtainMessage();
		          msg.obj = (isSuccess==true?"성공":"실패");
		          msg.arg1 = 2;
		          ftpHdr.sendMessage(msg);

		          // 성공시 파일 삭제
		          if ( isSuccess == true )
		        	  uploadFile.delete();
		          
              } catch(IOException e) {
                  e.printStackTrace();
                  isSuccess = false;
              } finally {
                  if (fis != null) {
                      try {fis.close(); } catch(IOException e) {}
                  }
              }//end try
          }//end for
          
          ftp.logout();
      } catch (Exception e) {
          e.printStackTrace();
      } finally {
          if (ftp != null && ftp.isConnected()) {
              try { ftp.disconnect(); } catch (IOException e) {}
          }
      }
      return isSuccess;
  }
}