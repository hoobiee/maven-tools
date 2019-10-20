package cn.home.maventool;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RuntimeUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.misc.JarFilter;

import java.io.*;
import java.util.StringTokenizer;

/**
 * @author lihzh-home
 */
public class Main {

    private static final Log log = LogFactory.getLog(Main.class);
    private static PropertyHelper propHelper = new PropertyHelper("config");
    private static Runtime _runRuntime = Runtime.getRuntime();
    private static boolean isDelete = Boolean.valueOf(propHelper.getValue("delete-installed-jar"));
    private static boolean isMove = Boolean.valueOf(propHelper.getValue("move-installed-jar"));
    private static final String KEY_JARPATH = "jar-path";
    private static final String KEY_BACKUPPATH = "back-path";
    private static final String ENCODE = "gbk";
    private static final String INSTALL_PATH = propHelper.getValue(KEY_JARPATH);
    private static String CMD_INSTALL_FILE;
    private static String CMD_BACKUP_JAR;

    public static void main(String[] args) {

        log.info("The path of the jars is-要导入maven本地jar包目录 :" + INSTALL_PATH);
        File file = new File(INSTALL_PATH);
        if (!file.isDirectory()) {
            log.warn("该路径是必须的配置config.properties  jar-path= : The path must be a directory.");
            return;
        }
        FilenameFilter filter = new JarFilter();
        File[] jarFiles = file.listFiles(filter);
        for (File jar : jarFiles) {
            installJarToMaven(jar);
            if (isDelete) {
                log.info("删除原始的jar文件 : Delete the original jar file [" + jar.getName() + "].");
                jar.delete();
            } else {
                if (isMove) {
                    log.info("移动原始的jar文件 : move the original jar file [" + jar.getName() + "].");
                    String backupPath = propHelper.getValue(KEY_BACKUPPATH);
                    backupJar(jar, file, backupPath);
                }
            }
        }
    }

    private static void backupJar(File jar, File file, String backupPath) {
        CMD_BACKUP_JAR = "copy " + INSTALL_PATH + File.separator + jar.getName() + " " + backupPath;
        String[] cmds = new String[]{"cmd", "/C", CMD_BACKUP_JAR};
        try {
            Process process = _runRuntime.exec(cmds, null, file);
            printResult(process);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("The jar [" + jar.getName() + "]  is backup, it's will be deleted.");
        jar.delete();

    }

	/**
	 * 安装Jar To Maven
	 * @param file
	 */
    private static void installJarToMaven(File file) {
        String fileName = file.getName();
        String jarName = getJarName(fileName);
        StringTokenizer strToken = new StringTokenizer(jarName, "-");
        String groupId = null;
        String artifactId = null;
        String version = null;
        if (strToken.hasMoreTokens()) {
            if (groupId == null) {
                groupId = strToken.nextToken();
                if (strToken.hasMoreTokens()) {
                    artifactId = strToken.nextToken();
                    if (strToken.hasMoreTokens()) {
                        version = strToken.nextToken();
                    }
                } else {
                    version = artifactId = groupId;
                }
            }
        }
        log.info("Jar [" + jarName + "] will be installed with the groupId=" + groupId + " ,"
                + "artifactId=" + artifactId + " , version=" + version + ".");
        executeInstall(groupId, artifactId, version, file.getPath());
    }

	/**
	 * 执行CMD命令安装
	 * @param groupId	对应为pom文件中的groupId
	 * @param artifactId	对应为pom文件中的artifactId
	 * @param version	对应为pom文件中的version
	 * @param path	路径
	 */
    private static void executeInstall(String groupId, String artifactId,
                                       String version, String path) {
        CMD_INSTALL_FILE = createInstallFileCMD(groupId, artifactId,
                version, path);
        String cmd = "cmd /c " + CMD_INSTALL_FILE;
        try {
            Process process = RuntimeUtil.exec(cmd);
            printResult(process);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	/**
	 * 打印处理结果
	 * @param process
	 * @throws IOException
	 */
    private static void printResult(Process process) throws IOException {
        InputStream is = process.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, ENCODE));
        String lineStr;
        while ((lineStr = br.readLine()) != null) {
            if (lineStr.contains("不是内部或外部命令,也不是可运行的程序")) {
                Console.log("需要IDEA右键管理员权限运行");
            }
            System.out.println(lineStr);
        }
    }

	/**
	 * 创建安装CMD命令
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param path
	 * @return
	 */
    private static String createInstallFileCMD(String groupId,
                                               String artifactId, String version, String path) {
        StringBuffer sb = new StringBuffer();
        sb.append("mvn install:install-file -DgroupId=").append(groupId)
                .append(" -DartifactId=").append(artifactId)
                .append(" -Dversion=").append(version)
                .append(" -Dpackaging=jar")
                .append(" -Dfile=").append(path);
        log.debug(sb.toString());
        return sb.toString();
    }

	/**
	 * 获取.jar前面名称
	 * @param fileName
	 * @return
	 */
	private static String getJarName(String fileName) {
        int index = fileName.indexOf(".jar");
        return fileName.substring(0, index);
    }

}
