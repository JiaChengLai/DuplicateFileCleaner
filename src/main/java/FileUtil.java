import org.junit.Test;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FileUtil {
    private static Map<Long, List<String>> checkedFileMap = new HashMap<Long, List<String>>();// key-文件大小 value-文件路径List
    public static int currentProcessedFileCount = 0;// 当前处理第几个文件
    public static boolean isCopyPathExist = false;// 被复制的目录是否存在

    /**
     * 获得文件的MD5
     */
    public static StringBuilder getFileMD5(File file) {
        StringBuilder result = new StringBuilder();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            FileInputStream in = new FileInputStream(file);
            BufferedInputStream bs = new BufferedInputStream(in);
            byte[] b = new byte[bs.available()]; //定义数组b为文件不受阻塞的可读取字节数

            //将文件以字节方式读取到数组b中
            while ((bs.read(b, 0, b.length)) != -1) ;
            md5.update(b);  //执行MD5算法
            for (byte by : md5.digest()) {
                result = result.append(String.format("%02X", by));   //将生成字节转为16进制的字符串
            }
            bs.close();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("无法进行Md5加密算法，可能是因为的java虚拟机版本太低");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("系统找不到指定的路径,请重新输入");
        } catch (IOException e) {
            throw new RuntimeException("IO错误");
        } catch (OutOfMemoryError e) {
            throw new RuntimeException("文件" + file.getName() + "过大，无法计算该文件的MD5值..");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 删除path文件夹及其子文件夹内所有重复文件
     * path-处理的文件夹路径 copyPath-重复的文件被移动到的文件夹
     */
    public static void deleteDuplicateFiles(String path, String copyPath) {
        File processedFile = new File(path);

        if (processedFile.isFile()) {// 判断文件类型，若为文件则报错
            throw new RuntimeException("deleteDuplicateFiles函数处理的对象应为文件夹!");
        }

        /*若processedFile为文件夹*/
        File[] processedFileList = processedFile.listFiles(); //得到文件夹中文件列表
        for (File processedListFile : processedFileList) {
            if (processedListFile.isDirectory()) { //如果列表项为文件夹则递归调用
                deleteDuplicateFiles(processedListFile.getPath(), copyPath);
            } else {
                System.out.println("正在处理第" + (++currentProcessedFileCount) + "个文件...");

                long fileSize = processedListFile.length();
                if (checkedFileMap.containsKey(fileSize)) {// 先判断是否有文件大小相同文件
                    boolean isDuplicated = false;

                    List<String> filePathList = checkedFileMap.get(fileSize);// 获取文件大小相同的文件list
                    /*比较文件大小相同的文件 是否Md5相同 若相同则是同一文件*/
                    String processedFileMd5 = getFileMD5(processedListFile).toString();
                    for (String filePath : filePathList) {
                        String fileMd5 = getFileMD5(new File(filePath)).toString();
                        if (processedFileMd5.equals(fileMd5)) {
                            isDuplicated = true;
                            break;
                        }
                    }
                    if (isDuplicated) {// 若该文件是重复文件
                        try {
                            cutFile(processedListFile, copyPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("文件 " + processedListFile.getName() + " 重复,已被删除...");
                    } else {// 若该文件不是重复文件
                        filePathList.add(processedListFile.getPath());
                    }
                } else {// 若map中不存在相同大小的文件
                    List<String> filePathList = new LinkedList<String>();
                    filePathList.add(processedListFile.getPath());
                    checkedFileMap.put(fileSize, filePathList);
                }
            }
        }
    }

    /**
     * 复制文件操作
     * file 被复制的文件 copyPath 复制到的路径
     */
    public static void copyFile(File file, String copyPath) throws IOException {
        if (!isCopyPathExist) { // 第一次复制时 判断所复制的目录是否存在
            if (!new File(copyPath).exists()) {
                new File(copyPath).mkdirs();// mkdir 创建指定目录, mkdirs 可创建多层不存在的目录
            }
            isCopyPathExist = true;
        }

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(copyPath + "/" + file.getName()));

        int len = 0;
        byte[] b = new byte[1024];
        while ((len = bis.read(b)) != -1) {
            bos.write(b, 0, len);
        }

        bis.close();
        bos.close();
    }

    /**
     * 剪切文件操作
     * file 被复制的文件 copyPath 剪切到的路径
     */
    public static void cutFile(File file, String cutPath) throws IOException {
        copyFile(file, cutPath);
        deleteFile(file);
    }

    /**
     * 根据file删除文件或文件夹
     */
    public static void deleteFile(File file) {
        if (file.isFile()) {// 若file为文件
            file.delete();
        } else {// 若File为文件夹
            File[] fileArr = file.listFiles();
            for (File processedFile : fileArr) {
                deleteFile(processedFile);
            }
            file.delete();
        }
    }

    @Test
    public void test() {
        deleteDuplicateFiles("D:\\data", "D:\\duplicateData");
    }
}
