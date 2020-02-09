package eci.edu.pgr.snap;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.apache.commons.collections.bag.SynchronizedSortedBag;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.interactions.internal.Coordinates;
import org.springframework.beans.factory.annotation.Value;

import javax.swing.plaf.basic.BasicDesktopIconUI;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SnapChat {

    private static Server server;
    private static AppiumDriver<MobileElement> driver;
    private static Thread sent;
    private static Thread receive;
    private static Socket socket;
    private static Process p = null;
    private static Logger LOGGER = Logger.getLogger(String.valueOf(SnapChat.class));

    private static List<MobileElement> recycleViewChildrens = null;

    private static boolean garbageNextMessage = false;


    @Value("${user}")
    private static String user = "p_ramirezxxx";

    @Value("${psw}")
    private static String psw = "pgr20191";


    public static void main(String[] args) throws MalformedURLException, InterruptedException {
        //runServerAppium();
        server = new Server();
        server.instanceServer();
        driver = server.getDriver();
        login();
        clickFirstChat();
        iniSocket();
        chat();
    }

    private static void runServerAppium() {
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", "appium");
        builder.redirectErrorStream(true);
        try {
            p = builder.start();
        } catch (IOException e) {
            System.err.println("ERROR A CREAR SERVIO DE APPIUM");
            e.printStackTrace();
        }
    }


    private static String reponseChat() {
        if (existParentsElements(ComponentName.recyleviewlist)) {
            recycleViewChildrens = driver.findElements(By.xpath(ComponentName.recyleviewlist));
            if(recycleViewChildrens.size()== 0 || recycleViewChildrens == null) return "";
            String lastMessage = "";
            int mobileElementIth = preProcess();
            if (mobileElementIth == -1) return "";
            while (mobileElementIth >= 0) {
                MobileElement mb = recycleViewChildrens.get(mobileElementIth);
                String className_ = (mb.getAttribute("className"));
                String className[] = className_.split("\\.");
                if (className.length == 3) {
                    LOGGER.info("Se va a procesar " + className[2]);
                    if (className[2].equals(ComponentName.LINEAR_LAYOUT)) {
                        if (isValid(mobileElementIth) && isXComponent(mobileElementIth, ComponentName.TEXT_VIEW)) {
                            MobileElement textView = recycleViewChildrens.get(mobileElementIth + 1);
                            String textOfView = textView.getAttribute("className");
                            System.out.println("Attribute " + textOfView + " " + textView.getText());
                            mobileElementIth++;
                            if (textView.getText().equals("Yo")) {
                                garbageNextMessage = true;
                            }
                        } else if (isValid(mobileElementIth) && isXComponent(mobileElementIth, ComponentName.VIEW)) {
                            String message = "";
                            do {
                                mobileElementIth += 2;// isJavaClass, next element is Vies and other element, we wait that will be javaClass
                                MobileElement javaClass = recycleViewChildrens.get(mobileElementIth);
                                if (javaClass.getAttribute("className").equals("javaClass")) {
                                    message = message + javaClass.getText() + '\n';
                                }
                                if(isValid(mobileElementIth) &&
                                        isXComponent(mobileElementIth, ComponentName.LINEAR_LAYOUT) &&
                                        isXComponent(mobileElementIth+1, ComponentName.VIEW)){
                                    mobileElementIth++;
                                }else{
                                    break;
                                }
                            } while (isValid(mobileElementIth));
                            if (garbageNextMessage) {
                                LOGGER.info(message.substring(0, message.length() - 1));
                                garbageNextMessage = false;
                            }
                            if(lastMessage.equals(message)){
                                return "";
                            }
                            lastMessage = message;
                            message = "";
                            return lastMessage;
                        }else if(isValid(mobileElementIth) && isXComponent(mobileElementIth, ComponentName.RELATIVE_LAYOUT)){
                                //HAY IMAGEN
                            try {
                                getScreenshot();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }


                }
                mobileElementIth++;
            }
        }
        return "";
    }

    public static void getScreenshot() throws IOException {
        System.out.println("Capturing the snapshot of the page ");
        File srcFiler=((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        FileUtils.copyFile(srcFiler, new File("C:\\Users\\user\\Documents\\"));
    }

    private static String splitElement(String className){
        String className_[] = className.split("\\.");
        return className_[className_.length-1];
    }


    private static int preProcess(){
       for(int i = recycleViewChildrens.size()-1; i>=0 ; i--){
            String attribute = recycleViewChildrens.get(i).getAttribute("className");
            if(splitElement(attribute).equals(ComponentName.TEXT_VIEW)){
                if(i-1>=0 && i+1 <recycleViewChildrens.size()){
                    String attributeBefore =   recycleViewChildrens.get(i-1).getAttribute("className");
                    if(splitElement(attributeBefore).equals(ComponentName.LINEAR_LAYOUT) && isXComponent(i,ComponentName.LINEAR_LAYOUT)){
                        return i-1;
                    }
                }
            }
        }
        return -1;

    }

    private static boolean isValid(int i){
        return ((i+1 < recycleViewChildrens.size()) ? true : false);
    }

    private static boolean isXComponent(int i , String component){
        if(!isValid(i)) return  false;
        String className[] = recycleViewChildrens.get(i+1).getAttribute("className").split("\\.");
        if(className.length ==3 ){
            return className[2].equals(component);
        }
        return false;
    }


    private static void iniSocket() {
        try {
            socket = new Socket("localhost", 9999);
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private static void write(String res){
        driver.findElement(By.id("com.snapchat.android:id/chat_input_text_field")).sendKeys(res);
        ((AndroidDriver) driver).pressKey(new KeyEvent(AndroidKey.ENTER));
    }


    private static boolean existParentsElements(String nameElment){
        try{
            List<MobileElement> recycleViewChildrens = driver.findElements(By.xpath(ComponentName.recyleviewlist));
            return true;
        }catch (Exception e){
            return false;
        }

    }

    private static void chat() {
        //write("Esto es un bot");
        sent = new Thread(
                new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader stdIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    while (true) {
                        String coversation = reponseChat();
                        if(coversation.length()>0){
                            out.print(coversation);
                            out.flush();
                            System.out.println("Trying to read...");
                            String in = stdIn.readLine();
                            System.out.println(in);
                            write(in);

                        }
                        try {
                            Thread.sleep(50000);
                        } catch (InterruptedException e) {
                            LOGGER.info("ERROR EN EL SPLEEP DEL HILO DE CHAT");
                        }


                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });
        sent.start();
        try {
            sent.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void clickFirstChat() throws InterruptedException {
        List<MobileElement> ele = driver.findElements(By.className("android.view.View"));
        ele.get(0).click();
        Thread.sleep(10000);

    }

    private static void login() throws InterruptedException {
        driver.findElement(By.id("com.snapchat.android:id/login_and_signup_page_fragment_login_button")).click();
        driver.findElement(By.id("com.snapchat.android:id/username_or_email_field")).setValue(user);
        driver.findElement(By.id("com.snapchat.android:id/password_field")).setValue(psw);
        driver.findElement(By.id("com.snapchat.android:id/button_text")).click();
        driver.findElement(By.id("com.snapchat.android:id/feed_icon_container")).click();
        Thread.sleep(30000);
    }
}
