package org.area515.resinprinter.uartscreen;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.network.WirelessNetwork;
import org.area515.resinprinter.uartscreen.Language;
//import org.area515.resinprinter.printer.Language;
import org.area515.resinprinter.printer.ParameterRecord;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.services.MachineService;
import org.area515.resinprinter.services.PrintableService;
import org.area515.resinprinter.services.PrinterService;
import org.area515.util.BasicUtillities;
import org.area515.util.IOUtilities;
import org.omg.PortableServer.ID_UNIQUENESS_POLICY_ID;

import com.google.common.primitives.Bytes;

/**
 * Created by zyd on 2017/8/10.
 * uart screen control
 */

public class UartScreenControl
{
    private String version = "0.6.02";  //derby on 2019-11-19

    //private int Page
    private Thread readThread;
    private Thread writeThread;
//    private Thread testThread;
    private volatile boolean isRead_stop = false;
    private volatile boolean isWrite_stop = false;

    private Printer printer;
    private BlockingQueue<byte[]> writeQueue;
    private int cur_file_selected = -1;
    private int cur_file_page = 0;
    private String cur_file_dir = null;

    private List<WirelessNetwork> network_list = null;
    private int cur_network_selected = -1;
    private int cur_network_page = 0;
    private String network_ssid;
    private String network_psk = "";

    private int numberOfFirstLayers;
    private int firstLayerTime;
    private int layerTime;
    private int resumeLayerTime;
    private double liftDistance;
    private double liftFeedSpeed;
    private double liftRetractSpeed;
    private int delayTimeBeforeSolidify;
    private int delayTimeAfterSolidify;
    private int delayTimeAsLiftedTop;
    private int delayTimeForAirPump;
    private boolean parameterEnabled;
    private boolean detectionEnabled;

    private boolean ledBoardEnabled = false;
    private boolean waterPumpEnabled = false;
    private boolean imageLogoEnabled = false;
    private boolean imageFullEnabled = false;

    private int ledPwmValue = 0;

    private String update_path = "/udiskdir/update-dlp";
//    private String update_path = "C:\\Users\\derby\\udiskdir\\update-dlp";
    private Timer shutterTimer;
    private char last_uiID = 0;


    /*****************machine status******************/
    private JobStatus machine_status = null;
    private String printFileName = "";
//    private long printFileSize = 0;
    private double printProgress = 0;
//    private int printCurrentLayer = 0;
    private int printTotalLayers = 0;
    private long printedTime = 0;
    private long remainingTime = 0;
    /*****************machine status******************/

    /***********************************/

    public UartScreenControl(Printer printer)
    {
        this.printer = printer;
        this.getLanguage();
        writeQueue = new ArrayBlockingQueue<byte[]>(64);
    }

    private void startReadThread()
    {
        readThread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                byte[] receive;
                char cmd;
                int key_value=0;

                while (!isRead_stop) {
                    try {
//                        if (getPrinter().getStatus().isNotReady()) {
//                            Thread.sleep(100);
//                            continue;
//                        }

                        receive = IOUtilities.read(getPrinter().getUartScreenSerialPort(), 2000, 10);
                        if (receive == null || receive.length < 9)
                            continue;
                        printBytes(receive);

                        cmd = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(receive, 3, 2));


                        if (cmd == UartScreenVar.addr_btn_file_ctrl )
                            action_file_ctrl(receive, true);
                        else if(cmd == UartScreenVar.addr_btn_file_usb_ctrl)
                        	action_file_ctrl(receive, false);
                        else if (cmd == UartScreenVar.addr_btn_mainUI || cmd == UartScreenVar.addr_btn_print_ctrl || 
                        		cmd == UartScreenVar.addr_btn_print_pause  || 
                        		cmd == UartScreenVar.addr_btn_print_suretoStop)
                            action_print_ctrl(receive);
                        else if (cmd == UartScreenVar.addr_btn_network)
                            action_network(receive);
                        else if (cmd == UartScreenVar.addr_btn_language)
                            action_language(receive);
                        else if (cmd == UartScreenVar.addr_btn_parameters)
                            action_parameters(receive);
                        else if (cmd == UartScreenVar.addr_btn_move_control)
                            action_move_control(receive);
                        else if (cmd == UartScreenVar.addr_btn_optical_control)
                            action_optical_control(receive);
                        else if (cmd == UartScreenVar.addr_btn_replace_part )
                            action_replace_part(receive);
                        else if (cmd == UartScreenVar.addr_btn_led_pwm_adjust)
                            action_led_pwm_adjust(receive);
                        else if (cmd == UartScreenVar.addr_btn_clear_trough )
                            action_clear_trough(receive);
                        else if (cmd == UartScreenVar.addr_btn_about)
                            action_about();
                        else if (cmd == UartScreenVar.addr_btn_update_software || cmd == UartScreenVar.addr_btn_update_software_failed)
                            action_update_software(receive);
                        else if (cmd == UartScreenVar.addr_btn_update_firmware || cmd == UartScreenVar.addr_btn_update_firmware_failed)
                            action_update_firmware(receive);
                        else if (cmd == UartScreenVar.addr_btn_network_psk)
                            action_set_network_psk(receive);
                        else if (cmd == UartScreenVar.addr_txt_admin_password)
                            action_set_admin_password(receive);
                        else if ( cmd == UartScreenVar.addr_btn_info_success || cmd == UartScreenVar.addr_btn_info_fail) {
                        	if(receive[2] == 0x11)
                            	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(receive, 5, 2));
                        	if(key_value == 2)
                        		goPage(last_uiID);
                        }
                        	
//                        else if (cmd >= UartScreenVar.addr_txt_parameters[0] &&
//                                cmd <= UartScreenVar.addr_txt_parameters[UartScreenVar.addr_txt_parameters.length - 1])
//                            action_parameters_set(receive);
//                        else if (cmd == UartScreenVar.addr_txt_led_pwm)
//                            action_set_led_pwm(receive);

                    }
                    catch (InterruptedException | IOException e) {
                        System.out.println(e.toString());
                    }
                }
                System.out.println("read thread stop");
            }
        });
        readThread.start();
    }

    private void startWriteThread()
    {
        writeThread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                byte[] bytes;
                while (!isWrite_stop) {
                    try {
                        bytes = writeQueue.poll(2000, TimeUnit.MILLISECONDS);
                        //bytes = writeQueue.take();
                        if (bytes == null || bytes.length <= 0)
                            continue;
                        getPrinter().getUartScreenSerialPort().write(bytes);
                    }
                    catch (InterruptedException | IOException e) {
                        System.out.println(e.toString());
                    }
                }
                System.out.println("write thread stop");
            }
        });
        writeThread.start();
    }

    /*
    private volatile boolean isTest_stop = true;
    private void startTestThread()
    {
        try
        {
            InputStream stream = new FileInputStream(new File("/opt/cwh/test.cfg"));
            Properties properties = new Properties();
            properties.load(stream);
            int openDelay = new Integer(properties.getProperty("openDelay", "5000"));
            int closeDelay = new Integer(properties.getProperty("closeDelay", "5000"));

            testThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    boolean flag = false;
                    while (!isTest_stop)
                    {
                        try
                        {
                            if (getPrinter().getStatus().isNotReady())
                            {
                                Thread.sleep(1000);
                                continue;
                            }
                            if (!flag)
                            {
                                getPrinter().getGCodeControl().executeShutterOn();
                                flag = true;
                            }
                            showImage("/opt/cwh/3DTALK.png");
                            Thread.sleep(openDelay);
                            showImage(null);
                            Thread.sleep(closeDelay);
                        } catch (InterruptedException e)
                        {
                            System.out.println(e.toString());
                        }
                    }
                    System.out.println("write thread stop");
                }
            });
            isTest_stop = false;
            testThread.start();
        }
        catch (IOException e){}
    }
    */

    public void start()
    {
        startReadThread();
        startWriteThread();
        
        int lang = getLanguage();
        byte[] bytes = new byte[] {(byte)0xEE, (byte)0xC1};
        if(lang == Language.CN.ordinal())
        	bytes = BasicUtillities.bytesCat(bytes, new byte[] {(byte)0x00, (byte)0xC1});
        else if(lang == Language.EN.ordinal()) {
        	bytes = BasicUtillities.bytesCat(bytes, new byte[] {(byte)0x01, (byte)0xC2});
        }
        bytes = BasicUtillities.bytesCat(bytes, new byte[] {(byte)0xFF, (byte)0xFC, (byte)0xFF, (byte)0xFF});
        try {
            writeQueue.put(bytes);
        }
        catch (InterruptedException e) {
            System.out.println(e.toString());
        }

        Main.GLOBAL_EXECUTOR.submit(new Runnable() {
            @Override
            public void run()
            {
                if (check_updatable() && HostProperties.Instance().isEnableUpdate()) {
                    close();
                    start_update();
                }
                else {
                    goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.Main));
                }
            }
        });
    }

    public void close()
    {
        isRead_stop = true;
        isWrite_stop = true;
        try {
            if (readThread != null) {
                readThread.join();
                readThread = null;
            }
            if (writeThread != null) {
                writeThread.join();
                writeThread = null;
            }
        }
        catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }

    public Printer getPrinter()
    {
        return this.printer;
    }

    private void writeTextWith2Address(char address[], byte[] content)
    {
        byte[] bytes_end = new byte[] {(byte)0xFF, (byte)0xFC, (byte)0xFF, (byte)0xFF};
        byte[] bytes = BasicUtillities.bytesCat(new byte[]{(byte)0xEE, (byte)0xB1, 0x10}, BasicUtillities.charToBytes(address[0]));
        bytes = BasicUtillities.bytesCat(bytes, BasicUtillities.charToBytes(address[1]));
        bytes = BasicUtillities.bytesCat(bytes, content);
        bytes = BasicUtillities.bytesCat(bytes, bytes_end);

        try {
            writeQueue.put(bytes);
        }
        catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }
    
    private void setTextColorWith2Address(char address[], byte[] content)
    {
    	byte[] bytes_end = new byte[] {(byte)0xFF, (byte)0xFC, (byte)0xFF, (byte)0xFF};
        byte[] bytes = BasicUtillities.bytesCat(new byte[]{(byte)0xEE, (byte)0xB1, 0x19}, BasicUtillities.charToBytes(address[0]));
        bytes = BasicUtillities.bytesCat(bytes, BasicUtillities.charToBytes(address[1]));
        bytes = BasicUtillities.bytesCat(bytes, content);
        bytes = BasicUtillities.bytesCat(bytes, bytes_end);
        printBytes(bytes);

        try {
            writeQueue.put(bytes);
        }
        catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }
    
    private void setIconWith2Address(char address[], byte[] content)
    {
    	byte[] bytes_end = new byte[] {(byte)0xFF, (byte)0xFC, (byte)0xFF, (byte)0xFF};
        byte[] bytes = BasicUtillities.bytesCat(new byte[]{(byte)0xEE, (byte)0xB1, 0x23}, BasicUtillities.charToBytes(address[0]));
        bytes = BasicUtillities.bytesCat(bytes, BasicUtillities.charToBytes(address[1]));
        bytes = BasicUtillities.bytesCat(bytes, content);
        bytes = BasicUtillities.bytesCat(bytes, bytes_end);

        try {
            writeQueue.put(bytes);
        }
        catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }
    
    private void setBtnWith2Address(char address[], byte[] content)
    {
    	byte[] bytes_end = new byte[] {(byte)0xFF, (byte)0xFC, (byte)0xFF, (byte)0xFF};
        byte[] bytes = BasicUtillities.bytesCat(new byte[]{(byte)0xEE, (byte)0xB1, 0x10}, BasicUtillities.charToBytes(address[0]));
        bytes = BasicUtillities.bytesCat(bytes, BasicUtillities.charToBytes(address[1]));
        bytes = BasicUtillities.bytesCat(bytes, content);
        bytes = BasicUtillities.bytesCat(bytes, bytes_end);

        try {
            writeQueue.put(bytes);
        }
        catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }
    
    
    private void writeText(char address, byte[] content)
    {
        
        byte[] bytes = new byte[]{(byte)0xEE, (byte)0xB1, 0x10, (byte) address};
        bytes = BasicUtillities.bytesCat(bytes, content);

        try {
            writeQueue.put(bytes);
        }
        catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }

    private void writeKey(byte key)
    {
        byte[] bytes = {0x5A, (byte)0xA5, 0x03, (byte)0x80, 0x4F, key};

        try {
            writeQueue.put(bytes);
        }
        catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }
    
    private void writeKey(Short x, Short y)
    {
        byte[] bytes = {0x5A, (byte)0xA5, 0x0B, (byte)0x82, 0x00, (byte)0xD4, 0x5A, (byte)0xA5, 0x00, 0x04};

        bytes = BasicUtillities.bytesCat(bytes, BasicUtillities.shortToBytes(x));
        bytes = BasicUtillities.bytesCat(bytes, BasicUtillities.shortToBytes(y));
        try {
            writeQueue.put(bytes);
        }
        catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }

    private void goPage(int page)
    {
        byte[] bytes;
        
        ////modified by derby for new dwin 10.1 panel
        //bytes = new byte[]{0x5A, (byte) 0xA5, 0x04, (byte) 0x80, 0x03, 0x00, (byte) page};
        bytes = new byte[]{(byte)0xEE, (byte) 0xB1, 0x00, 0x00, (byte) page, (byte)0xFF, (byte) 0xFC, (byte)0xFF,(byte)0xFF};

        try {
            writeQueue.put(bytes);
        }
        catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }

    private int getLanguage()
    {
        return HostProperties.Instance().getParameterRecord().getLanguage();
    }
    
    private String getModelNumber()
    {
        return HostProperties.Instance().getModelNumber();
    }

    private void printBytes(byte[] bytes)
    {
        String str = "";
        for (byte b : bytes) {
            str += String.format("0x%02x,", b);
        }
        System.out.println(str);
    }

    public void setError(JobStatus status)
    {
        try {
            if (status == JobStatus.ErrorScreen)
                writeTextWith2Address(UartScreenVar.addr_txt_machineStatus, String.format("%-32s", new String(new char[] {0x5C4F, 0x5E55, 0x9519, 0x8BEF})).getBytes("GBK"));//鐏炲繐绠烽柨娆掝嚖
            else if (status == JobStatus.ErrorControlBoard)
            	writeTextWith2Address(UartScreenVar.addr_txt_machineStatus, String.format("%-32s", new String(new char[] {0x63A7, 0x5236, 0x7248, 0x9519, 0x8BEF})).getBytes("GBK"));//閹貉冨煑閺夊潡鏁婄拠锟�
            while (!writeQueue.isEmpty())
                Thread.sleep(100);
        }
        catch (UnsupportedEncodingException | InterruptedException e) {}
    }

    private List<String> getPrintableList(String whichDir)
    {
        return PrintableService.INSTANCE.getPrintableFiles(whichDir);
    }

    private void filesUpdate(String whichDir, int selected)
    {
        List<String> files = getPrintableList(whichDir);
        String file;
        int fileCnt = 0;
        if(getModelNumber().equals("3DTALK_DS200")) 
       	 	fileCnt = 5;
    	else if(getModelNumber().equals("3DTALK_DF200") || getModelNumber().equals("3DTALK_DS200_MONO"))
    		fileCnt = 4;
    	else 
       	 	fileCnt = 5;
       

        if (selected < 0)
            selected = 0;

        if (files.size() == 0)
            cur_file_selected = -1;
        else if (selected >= files.size() - 1)
            cur_file_selected = files.size() - 1;
        else
            cur_file_selected = selected;

        if (cur_file_selected < 0)
            cur_file_page = 0;
        else
            cur_file_page = cur_file_selected / fileCnt;

        cur_file_dir = whichDir;

        for (int i = 0; i < fileCnt; i++) {
            file = "";
            if (files.size() > i + cur_file_page * fileCnt) {
                file = files.get(i + cur_file_page * fileCnt);
            }
            try {
               	byte[] sendString = String.format("%-64s", file).getBytes("UTF-16BE");
               	if(whichDir.equals("udisk"))
               		writeTextWith2Address(UartScreenVar.addr_txt_usb_fileList[i], sendString);//String.format("%-32s", file).getBytes("Unicode"));
               	else {
               		writeTextWith2Address(UartScreenVar.addr_txt_fileList[i], sendString);//String.format("%-32s", file).getBytes("Unicode"));
				}
            }
            catch (UnsupportedEncodingException e) {
                System.out.println(e.toString());
            }
        }
//        clearProgBar(getModelNumber());
//        showFilePageNumber();

        fileHighLight(cur_file_selected,getModelNumber());
    }

    private void fileHighLight(int selected,String modelNum) //modify by derby 2020/1/14
    {
        if (selected < 0)
            return;

        
        int fileCnt = 0;
        if(getModelNumber().equals("3DTALK_DS200")) 
       	 	fileCnt = 5;
    	else if(modelNum.equals("3DTALK_DF200") || getModelNumber().equals("3DTALK_DS200_MONO"))
    		fileCnt = 4;
    	else 
       	 	fileCnt = 5;
    	
    	selected = selected % fileCnt;

        for (int i = 0; i < fileCnt; i++) {
        	if(modelNum.equals("3DTALK_DF200") || getModelNumber().equals("3DTALK_DS200_MONO")) {
	            if (selected == i) {
	            	setTextColorWith2Address(UartScreenVar.addr_txt_fileList[i], new byte[] {(byte)0xF8,0x00});
	            	setTextColorWith2Address(UartScreenVar.addr_txt_usb_fileList[i], new byte[] {(byte)0xF8,0x00}); //the second param is text's color
	            }
	            else {
	            	setTextColorWith2Address(UartScreenVar.addr_txt_fileList[i], new byte[] {(byte)0x00,0x00});
	            	setTextColorWith2Address(UartScreenVar.addr_txt_usb_fileList[i], new byte[] {(byte)0xF00,0x00}); //the second param is text's color
	            }
	            	
        	}
        	else if(getModelNumber().equals("3DTALK_DS200"))  {
        		if (selected == i)
        			setTextColorWith2Address(UartScreenVar.addr_txt_fileList[i], new byte[] {(byte)0xF8}); //the second param is text's color
	            else
	            	setTextColorWith2Address(UartScreenVar.addr_txt_fileList[i], new byte[] {(byte)0xF8});
        	}
        }
    }

    private void clearProgBar(String modelNum)
    {
    	int fileCnt = 0;
    	if(getModelNumber().equals("3DTALK_DS200")) 
       	 	fileCnt = 5;
    	else if(modelNum.equals("3DTALK_DF200") || getModelNumber().equals("3DTALK_DS200_MONO"))
    		fileCnt = 4;
    	else {
       	 fileCnt = 5;
       }
        for (int i = 0; i < fileCnt; i++) {
            writeText(UartScreenVar.addr_icon_prog[i], new byte[] {0x00, (byte)65});
        }
    }

    private void showFilePageNumber()
    {
        writeText(UartScreenVar.addr_txt_filePage, new byte[] { (byte) ((cur_file_page >> 8) & 0xFF), (byte) (cur_file_page & 0xFF)});
    }

    private String fileCopy()
    {
        String filename = null;

        List<String> files = getPrintableList("udisk");

        if (files.size() > 0 && cur_file_selected <= files.size() - 1 && cur_file_selected >= 0) {
            filename = files.get(cur_file_selected);
            uploadFromUdiskToLocal(filename);
        }
        return filename;
    }

    private void fileDelete()
    {
        List<String> files = getPrintableList("local");

        if (files.size() > 0 && cur_file_selected <= files.size() - 1 && cur_file_selected >= 0) {
            deleteLocalFile(files.get(cur_file_selected));
            filesUpdate("local", cur_file_selected);
        }
    }

    private void uploadFromUdiskToLocal(String fileName)
    {
    	
        PrintableService.INSTANCE.uploadViaUdisk(fileName, new ProgressCallback() {
            @Override
            public void onProgress(double progress)
            {
            	int fileCnt = 0;
            	if(getModelNumber().equals("3DTALK_DS200")) 
            		fileCnt = 5;
            	else if(getModelNumber().equals("3DTALK_DF200") || getModelNumber().equals("3DTALK_DS200_MONO"))
            		fileCnt = 4;
            	else
            		fileCnt = 5;
                writeText(UartScreenVar.addr_icon_prog[cur_file_selected % fileCnt], new byte[] {0x00, (byte)(39 + progress / 4)});
                System.out.println(progress);
            }
        });
    }

    private void deleteLocalFile(String fileName)
    {
        PrintableService.INSTANCE.deleteFile(fileName);
    }

    private void jobPrint(String fileName)
    {
        Printer printer = getPrinter();
        if (printer.isStarted() && !printer.isPrintInProgress()) {
            PrinterService.INSTANCE.print(fileName, printer.getName());
        }
    }

    private void jobPause()
    {
        Printer printer = getPrinter();
        printer.togglePause();
        setMachineStatus(printer.getStatus(), false, false);
    }

    private void jobStop()
    {
        Printer printer = getPrinter();
        if (printer.isPrintActive()) {
            printer.setStatus(JobStatus.Cancelling);
            setMachineStatus(printer.getStatus(), false, false);
        }
    }

    private void printJob()
    {
        String filename = null;
        if (this.cur_file_dir.equals("udisk")) {
            filename = fileCopy();
        }
        else {
            List<String> files = getPrintableList("local");
            if (files.size() > 0 && cur_file_selected <= files.size() - 1 && cur_file_selected >= 0) {
                filename = files.get(cur_file_selected);
            }
        }

        if (filename != null) {
            jobPrint(filename);
            goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.Main));
            setIconWith2Address(UartScreenVar.addr_icon_pause, new byte[] {0x02});
            setIconWith2Address(UartScreenVar.addr_icon_stop, new byte[] {0x03});
        }
    }

    private void pauseJob()
    {
        jobPause();
//        setIconWith2Address(UartScreenVar.addr_icon_pause, new byte[] {0x01});
    }

    private void stopJob()
    {
        jobStop();
        goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.Main));
//        setIconWith2Address(UartScreenVar.addr_icon_pause, new byte[] {0x04});
//        setIconWith2Address(UartScreenVar.addr_icon_stop, new byte[] {0x04});
    }

    private List<WirelessNetwork> getNetworks()
    {
    	//mock wifi list
//    	List<WirelessNetwork> wifiList = new ArrayList<WirelessNetwork>();
//    	for(int i=0;i<10;i++) {
//    		WirelessNetwork w1 = new WirelessNetwork();
//    		w1.setSsid("wifi"+ i);
//    		wifiList.add(w1);
//    	}
//    	WirelessNetwork w1 = new WirelessNetwork();
//    	w1.setSsid("wifi1");
//    	return wifiList;
        return MachineService.INSTANCE.getWirelessNetworks();
    }

    private void networksUpdate()
    {
        network_list = getNetworks();
        networkSelect(0);
    }

    private void networkSelect(int selected)
    {
        String network;

        if (selected < 0)
            selected = 0;

        if (network_list == null || network_list.size() == 0)
            cur_network_selected = -1;
        else if (selected >= network_list.size() - 1)
            cur_network_selected = network_list.size() - 1;
        else
            cur_network_selected = selected;

        if (cur_network_selected < 0)
            cur_network_page = 0;
        else
            cur_network_page = cur_network_selected / 5;

        //System.out.println("list networks");
        for (int i = 0; i < 5; i++) {
            network = "";
            if (network_list != null && network_list.size() > i + cur_network_page * 5) {
                network = network_list.get(i + cur_network_page * 5).getSsid();
            }
            try {
            	writeTextWith2Address(UartScreenVar.addr_txt_network_List[i], String.format("%-32s", network).getBytes("UTF-16BE"));
                //System.out.println(network);
            }
            catch (UnsupportedEncodingException e) {
                System.out.println(e.toString());
            }
        }

        networkHighLight(cur_network_selected);
        if (cur_network_selected < 0)
            setNetworkSsid("");
        else
            setNetworkSsid(network_list.get(cur_network_selected).getSsid());
    }

    private void networkHighLight(int selected)
    {
        if (selected < 0)
            return;

        selected = selected % 5;

        for (int i = 0; i < 5; i++) {
            if (selected == i)
            	setTextColorWith2Address(UartScreenVar.addr_txt_network_List[i], new byte[] {(byte)0xF8,0x00}); //the second param is text's color
            else
            	setTextColorWith2Address(UartScreenVar.addr_txt_network_List[i], new byte[] {(byte)0x00,0x00});
        }
    }

    private void connectNetwork(String ssid, String psk)
    {
        boolean hasSsid = false;
        goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.connecting));
        for (WirelessNetwork network : getNetworks()) {
            if (network.getSsid().equals(ssid)) {
                hasSsid = true;
                network.setPassword(psk);
                if (MachineService.INSTANCE.connectToWifiSSID(network)) {
                    Main.GLOBAL_EXECUTOR.submit(new Runnable() {
                        @Override
                        public void run()
                        {
                            try {
                                int count = 10;
                                String ipAddress;
                                while (count-- > 0) {
                                    Thread.sleep(3000);
                                    ipAddress = getIpAddress();
                                    if (ipAddress != null) {
                                    	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.operation_success));
                                        break;
                                    }
                                    else if (count == 0) {
                                    	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.operation_fail));
                                    }
                                }
                            }
                            catch (InterruptedException e) {
                                System.out.println(e.toString());
                            }
                        }
                    });
                }
                else
                {
                	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.operation_fail));
                    try {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException e) {
                        System.out.println(e.toString());
                    }
//                    writeKey((short)66,(short)16);
                }
                break;
            }
        }
        if (!hasSsid) {
        	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.operation_fail));
//            writeKey((short)16,(short)16);
//            try {
//                Thread.sleep(500);
//            }
//            catch (InterruptedException e) {
//                System.out.println(e.toString());
//            }
//            writeKey((short)66,(short)16);
        }
    }

    private String getIpAddress()
    {
        return MachineService.INSTANCE.getLocalIpAddress("wlan0");
    }

    private String getNetworkSsid()
    {
        return this.network_ssid;
    }

    private void setNetworkSsid(String ssid)
    {
        this.network_ssid = ssid;
        try {
        	writeTextWith2Address(UartScreenVar.addr_txt_networkSsid, String.format("%-32s", ssid).getBytes("UTF-16BE"));
		} catch (UnsupportedEncodingException e) {
			
			System.out.println(e.toString());
		}
    }

    private String getNetworkPsk()
    {
        return this.network_psk;
    }

    private void setNetworkPsk(String psk)
    {
        this.network_psk = psk;
    }

    private void readParameters()
    {
        numberOfFirstLayers = getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getNumberOfFirstLayers();
        firstLayerTime = getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getFirstLayerExposureTime();
        layerTime = getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getExposureTime();
        resumeLayerTime = getPrinter().getConfiguration().getSlicingProfile().getResumeLayerExposureTime();
        liftDistance = getPrinter().getConfiguration().getSlicingProfile().getLiftDistance();
        liftFeedSpeed = getPrinter().getConfiguration().getSlicingProfile().getLiftFeedSpeed();
        liftRetractSpeed = getPrinter().getConfiguration().getSlicingProfile().getLiftRetractSpeed();
        delayTimeBeforeSolidify = getPrinter().getConfiguration().getSlicingProfile().getDelayTimeBeforeSolidify();
        delayTimeAfterSolidify = getPrinter().getConfiguration().getSlicingProfile().getDelayTimeAfterSolidify();
        delayTimeAsLiftedTop = getPrinter().getConfiguration().getSlicingProfile().getDelayTimeAsLiftedTop();
        delayTimeForAirPump = getPrinter().getConfiguration().getSlicingProfile().getDelayTimeForAirPump();
        parameterEnabled = getPrinter().getConfiguration().getSlicingProfile().getParameterEnabled();
        detectionEnabled = getPrinter().getConfiguration().getSlicingProfile().getDetectionEnabled();
        
        String strNumber = Integer.toString(numberOfFirstLayers);

        writeTextWith2Address(UartScreenVar.addr_txt_parameters[0], strNumber.getBytes());
        writeTextWith2Address(UartScreenVar.addr_txt_parameters[1], Integer.toString(firstLayerTime).getBytes());
        writeTextWith2Address(UartScreenVar.addr_txt_parameters[2], Integer.toString(layerTime).getBytes());
        writeTextWith2Address(UartScreenVar.addr_txt_parameters[3], Integer.toString(resumeLayerTime).getBytes());
        writeTextWith2Address(UartScreenVar.addr_txt_parameters[4], Double.toString(liftDistance).getBytes());
        writeTextWith2Address(UartScreenVar.addr_txt_parameters[5], Double.toString(liftFeedSpeed).getBytes());
        writeTextWith2Address(UartScreenVar.addr_txt_parameters[6], Double.toString(liftRetractSpeed).getBytes());
        writeTextWith2Address(UartScreenVar.addr_txt_parameters[7], Integer.toString(delayTimeBeforeSolidify).getBytes());
        writeTextWith2Address(UartScreenVar.addr_txt_parameters[8], Integer.toString(delayTimeAfterSolidify).getBytes());
        writeTextWith2Address(UartScreenVar.addr_txt_parameters[9], Integer.toString(delayTimeAsLiftedTop).getBytes());
        //writeTextWith2Address(UartScreenVar.addr_txt_parameters[10], new byte[] { (byte) ((delayTimeForAirPump >> 8) & 0xFF), (byte) (delayTimeForAirPump & 0xFF)});
        if (parameterEnabled)
        	setBtnWith2Address(UartScreenVar.addr_icon_parameter_enabled, new byte[] {0x01});
        else
        	setBtnWith2Address(UartScreenVar.addr_icon_parameter_enabled, new byte[] {0x00});
        if (detectionEnabled)
            writeText(UartScreenVar.addr_icon_detection_enabled, new byte[] {0x00, 67});
        else
            writeText(UartScreenVar.addr_icon_detection_enabled, new byte[] {0x00, 66});
    }

    private void saveParameters()
    {
        getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().setNumberOfFirstLayers(numberOfFirstLayers);
        getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().setFirstLayerExposureTime(firstLayerTime);
        getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().setExposureTime(layerTime);
        getPrinter().getConfiguration().getSlicingProfile().setResumeLayerExposureTime(resumeLayerTime);
        getPrinter().getConfiguration().getSlicingProfile().setLiftDistance(liftDistance);
        getPrinter().getConfiguration().getSlicingProfile().setLiftFeedSpeed(liftFeedSpeed);
        getPrinter().getConfiguration().getSlicingProfile().setLiftRetractSpeed(liftRetractSpeed);
        getPrinter().getConfiguration().getSlicingProfile().setDelayTimeBeforeSolidify(delayTimeBeforeSolidify);
        getPrinter().getConfiguration().getSlicingProfile().setDelayTimeAfterSolidify(delayTimeAfterSolidify);
        getPrinter().getConfiguration().getSlicingProfile().setDelayTimeAsLiftedTop(delayTimeAsLiftedTop);
        getPrinter().getConfiguration().getSlicingProfile().setDelayTimeForAirPump(delayTimeForAirPump);
        getPrinter().getConfiguration().getSlicingProfile().setParameterEnabled(parameterEnabled);
        getPrinter().getConfiguration().getSlicingProfile().setDetectionEnabled(detectionEnabled);
        PrinterService.INSTANCE.savePrinter(getPrinter());
    }

    private void setVersion(String version, char[] type)
    {
    	writeTextWith2Address(type, String.format("%-10s", version).getBytes());
    }

    private void setLiftTime()
    {
        String string;

        long ledUsedTime = getPrinter().getLedUsedTime();
        string = String.format("%.1f/%d", ledUsedTime/(60*60*1000.0), 5000);
        double led_num = 5 - ledUsedTime / (60*60*1000*5000)*100/20;
        writeTextWith2Address(UartScreenVar.addr_txt_lifetime_led, String.format("%-10s", string).getBytes());
        setIconWith2Address(UartScreenVar.addr_icon_lifetime_led, new byte[] {(byte)(5 - ledUsedTime / (60*60*1000.0*5000)*100/20)}); //add by derby 2020/1/14 led_life icon

        long screenUsedTime = getPrinter().getScreenUsedTime();
        string = String.format("%.1f/%d", screenUsedTime/(60*60*1000.0), 2000);
        writeTextWith2Address(UartScreenVar.addr_txt_lifetime_screen, String.format("%-10s", string).getBytes());
        setIconWith2Address(UartScreenVar.addr_icon_lifetime_screen, new byte[] {(byte)(5 - screenUsedTime / (60*60*1000.0*2000)*100/20)}); //add by derby 2020/1/14 screen_life icon
    }

    private void loadAdminAccount(String password)
    {
//        writeText(UartScreenVar.addr_txt_admin_password, String.format("%-16s", "").getBytes());
        if (password.equals("123")) {
            goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.Admin));
//            setLiftTime();
        }
        else {
        	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.operation_fail));
        }
    }

    private void showImage(String filePath)
    {
        try {
            if (filePath != null && BasicUtillities.isExists(filePath)) {
                if (getPrinter().getConfiguration().getSlicingProfile().getDetectionLiquidLevelEnabled()) {
                    if (getPrinter().getGCodeControl().executeDetectLiquidLevel().equals("H"))
                        return;
                }

                IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "sudo xset s off"}, null);
                IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "sudo xset -dpms"}, null);
                IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "sudo xset s noblank"}, null);

                File imageFile = new File(filePath);
                BufferedImage image = ImageIO.read(imageFile);
                getPrinter().showImage(image);
            }
            else {
                getPrinter().showBlankImage();
            }
        }
        catch (IOException e) {
            System.out.print(e.toString());
        }
    }

    /****************************notify uartscreen state -start*************************************/
    public void notifyState(Printer printer, PrintJob job)
    {
        setMachineStatus(printer.getStatus(), false, false);

        if (job != null) {
            setPrintFileName(job.getJobName(), false, false);
            setPrintProgress(job.getJobProgress(), true, false);
            setPrintTime(job.getElapsedTime(), job.getEstimateTimeRemaining(), true, false);
        }
        else {
            setPrintFileName("", false, true);
            setPrintProgress(0, false, true);
            setPrintTime(0, 0, true, false);
        }
    }

    private void setMachineStatus(JobStatus status, boolean force, boolean hide)
    {
        if (this.machine_status != status) {
            this.machine_status = status;
            force = true;
        }

        if (hide) {
        	writeTextWith2Address(UartScreenVar.addr_txt_machineStatus, String.format("%-32s", "").getBytes());
        }
        else if (force) {
            String string;
            if (getLanguage() == Language.RU.ordinal())
                string = status.getStateStringRU();
            else if(getLanguage() == Language.CN.ordinal())
                string = status.getStateStringCN();
            else
            	string = status.getStateString();//add by debry 2020/1/14
            
            //System.out.println(string+getLanguage());
        

            try {
            	writeTextWith2Address(UartScreenVar.addr_txt_machineStatus, String.format("%-32s", string).getBytes("UTF-16")); //derby 1-14
                if (status == JobStatus.Printing) 
                	setIconWith2Address(UartScreenVar.addr_icon_pause, new byte[] {0x02});
                    
                else if (status.isPaused()) 
                	setIconWith2Address(UartScreenVar.addr_icon_pause, new byte[] {0x01});
                else
                	setIconWith2Address(UartScreenVar.addr_icon_pause, new byte[] {0x04});
                if (status.isPrintActive())
                	setIconWith2Address(UartScreenVar.addr_icon_stop, new byte[] {0x03});
                else
                	setIconWith2Address(UartScreenVar.addr_icon_stop, new byte[] {0x04});
            }
            catch (UnsupportedEncodingException e) {
                System.out.println(e.toString());
            }
        }  
        }
    

    private void setPrintFileName(String fileName, boolean force, boolean hide)
    {
        if (!this.printFileName.equals(fileName)) {
            this.printFileName = fileName;
            force = true;
        }

        if (hide) {
            try {
            	writeTextWith2Address(UartScreenVar.addr_txt_printFileName, String.format("%-32s", fileName).getBytes("UTF-16BE"));
			} catch (UnsupportedEncodingException e) {
				System.out.println(e.toString());
			}
        }
        else if (force) {
            try {
            	writeTextWith2Address(UartScreenVar.addr_txt_printFileName, String.format("%-32s", this.printFileName).getBytes("UTF-16BE")); //derby 1-14
            }
            catch (UnsupportedEncodingException e) {
                System.out.println(e.toString());
            }
        }
    }

    private void setPrintProgress(double printProgress, boolean force, boolean hide)
    {
        if (this.printProgress != printProgress) {
            this.printProgress = printProgress;
            force = true;
        }

        if (hide) {
        	writeTextWith2Address(UartScreenVar.addr_txt_printProgress, String.format("%-10s", "").getBytes());
        	setIconWith2Address(UartScreenVar.addr_icon_printProgress, new byte[] {0x00});
            if(getModelNumber().equals("3DTALK_DS200_MONO")){
            	writeText(UartScreenVar.addr_icon_printProgress_ex, new byte[] {0x00, (byte)(83)});
//            	writeText(UartScreenVar.addr_icon_printProgress, new byte[] {0x00, (byte)(78)});
            }
        }
        else if (force) {
            String string = String.format("%.1f%%", printProgress);
            writeTextWith2Address(UartScreenVar.addr_txt_printProgress, String.format("%-10s", string).getBytes());
//            if(getModelNumber().equals("3DTALK_DS200_MONO")){
//            	if(printProgress < 60) {
//                	writeText(UartScreenVar.addr_icon_printProgress, new byte[] {0x00, (byte)(79 + printProgress / 20)}); //add by derby 2020/1/14 progress icon
//                	writeText(UartScreenVar.addr_icon_printProgress_ex, new byte[] {0x00, (byte)(83)});
//                }
//                else if(printProgress >= 60 && printProgress < 100){
//                	writeText(UartScreenVar.addr_icon_printProgress_ex, new byte[] {0x00, (byte)(84 + (printProgress-60) / 20)}); //add by derby 2020/9/24 for ds300
//    			}
//            }
//            else
            	setIconWith2Address(UartScreenVar.addr_icon_printProgress, new byte[] {(byte)(printProgress / 20)}); //add by derby 2020/1/14 progress icon
        }
    }

    private void setPrintTime(long printedTime, long remainingTime, boolean force, boolean hide)
    {
        if (this.printedTime != printedTime || this.remainingTime != remainingTime) {
            this.printedTime = printedTime;
            this.remainingTime = remainingTime;
            force = true;
        }

        if (hide) {
        	
            //writeText(UartScreenVar.addr_icon_printTime, String.format("%-32s", "").getBytes());
        }
        else if (force) {
        	if(getModelNumber().equals("3DTALK_DS200"))  {
        		String string = String.format("%d:%02d:%02d / %d:%02d:%02d",
                        this.printedTime / 3600000,
                        (this.printedTime % 3600000) / 60000,
                        (this.printedTime % 60000) / 1000,
                        this.remainingTime / 3600000,
                        (this.remainingTime % 3600000) / 60000,
                        (this.remainingTime % 60000) / 1000);
                writeText(UartScreenVar.addr_txt_printTime, String.format("%-32s", string).getBytes());
        	}
        	else if(getModelNumber().equals("3DTALK_DF200") || getModelNumber().equals("3DTALK_DS200_MONO")) {
        		//add by derby 2020/2/18 printTime by icon
        		long[] timeArray = {this.remainingTime/3600000/10,//hour high bit
            			(this.remainingTime/3600000)%10,	//hour lower bit
            			(this.remainingTime%3600000)/60000/10,  //min_H
            			((this.remainingTime%3600000)/60000)%10,	//min_L
            			(this.remainingTime%60000)/1000/10,	//sec_H
            			((this.remainingTime%60000)/1000)%10};	//sec_L
            	
            	for(int i=0;i<7;i++) {
            		setIconWith2Address(UartScreenVar.addr_icon_printTime[i], new byte[] {(byte)(timeArray[i])});
        	}
        	
        	
        	}
        }
    }
    /****************************notify uartscreen state -end*************************************/

    /***************************action function -start**************************************/
    private void action_file_ctrl(byte[] payload, boolean isLocalfile)
    {
        if (payload.length < 9)
            return;
        int fileCnt = 0;
    	if(getModelNumber().equals("3DTALK_DS200")) 
       	 	fileCnt = 5;
    	else if(getModelNumber().equals("3DTALK_DF200") || getModelNumber().equals("3DTALK_DS200_MONO"))
    		fileCnt = 4;
    	else 
       	 	fileCnt = 5;
    	
    	char cmd = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));
        int key_value = 0;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));
        else if(payload[2] == 0x01)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 14, 2));
        String cur_dir = "local";
        if(!isLocalfile)
        	cur_dir = "udisk";

        if(key_value >= 19 && key_value <= 22) //select file
            filesUpdate(cur_dir, key_value-19+cur_file_page*fileCnt);
        else if ((key_value == 0x0006 || key_value ==0x0002) && isLocalfile) //local file
            filesUpdate("local", 0);
        else if (key_value == 0x0006 && !isLocalfile) //udisk file
            filesUpdate("udisk", 0);
        else if (key_value == 0x0007 ) //up
            filesUpdate(cur_file_dir, cur_file_selected-fileCnt);
        else if (key_value == 0x0008 ) //down
            filesUpdate(cur_file_dir, cur_file_selected+fileCnt );
        else if (key_value == 0x000A && isLocalfile) { 
            if (getPrinter().getStatus().isPrintInProgress())
                return;
            fileDelete();
        }
        else if (key_value == 0x000A && !isLocalfile) { //copy
            if (getPrinter().getStatus().isPrintInProgress())
                return;
            fileCopy();
        }

    }

    private void action_print_ctrl(byte[] payload)
    {
        if (payload.length < 9)
            return;
        char cmd = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 3, 2));
        int key_value = 0;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));

        if (cmd == UartScreenVar.addr_btn_print_ctrl && key_value == 2 && !getPrinter().getStatus().isPrintInProgress())
            printJob();
        else if (cmd == UartScreenVar.addr_btn_mainUI && key_value == 18)
            pauseJob();
        else if (cmd == UartScreenVar.addr_btn_mainUI && key_value == 29 ) {
        	if (getPrinter().getStatus().isPrintInProgress())
        		goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.sureToStop));
        }
        	
        else if (cmd == UartScreenVar.addr_btn_print_suretoStop && key_value == 2)
            stopJob();
    }

    private void action_network(byte[] payload)
    {
        if (payload.length < 9)
            return;

        char cmd = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 3, 2));
        int key_value = 0;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));
        else if(payload[2] == 0x01)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 14, 2));
       

        if (key_value >= 17 && key_value <= 22)
            networkSelect(cur_network_page * 5 + key_value - 17);
        else if (key_value == 13)
            networkSelect(cur_network_selected - 5);
        else if (key_value == 14)
            networkSelect(cur_network_selected + 5);
        else if (key_value == 11) {  //typein wifi password
            if (cur_network_selected >= 0) {
                goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.NetworkEdit));
                writeTextWith2Address(UartScreenVar.addr_txt_networkSsid, getNetworkSsid().getBytes());
            }
        }
        else if (key_value == 12 || key_value == 6)  //update wifi list id14
            networksUpdate();
//        else if (key_value == 0x09) {
//            String psk = getNetworkPsk();
//            System.out.println(psk);
//            if (psk.length() >= 8)
//                connectNetwork(getNetworkSsid(), getNetworkPsk());
//            else
//            	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.operation_fail));
//        }
        last_uiID = cmd;
    }

    private void action_set_network_psk(byte[] payload)
    {
        //String psk = new String(BasicUtillities.subBytes(payload, 7));
        //setNetworkPsk(psk.replaceAll("[^\\x20-\\x7E]", ""));
    	
    	//modified by derby 2021/3/8 for new dwin screen.
//        String psk = new String(BasicUtillities.subBytes(payload, 9, payload[8])); 
        int key_value = 0;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));
        else if(payload[2] == 0x01)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 14, 2));
    	if (key_value == 7) {
	        String psk = new String(BasicUtillities.subBytes(payload,8,payload.length-8-5)); 
	        //System.out.println(psk);
	        setNetworkPsk(psk.replaceAll("[^\\x20-\\x7E]", ""));
	        if (psk.length() >= 8)
	            connectNetwork(getNetworkSsid(), getNetworkPsk());
    	}
    }

    private void action_parameters(byte[] payload)
    {
        if (payload.length < 9)
            return;

        int key_value = 0;
        char cmd = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 3, 2));
        String strRecv;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));

        if (key_value == 0) { //鐠囪褰囬崣鍌涙殶
            readParameters();
        }
        if (key_value == 16) { //save parameter
            if (getPrinter().getStatus().isPrintInProgress()) {
            	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.no_operation));
            	try {
					wait(500);
				} catch (Exception e) {
					// TODO: handle exception
				}
            	goPage(last_uiID);
                return;
            }
            saveParameters();
            goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.operation_success));
        }
        else if (key_value == 17) {
            parameterEnabled = !parameterEnabled;
            if (parameterEnabled)
            	setBtnWith2Address(UartScreenVar.addr_icon_parameter_enabled, new byte[] {0x01});
            else
            	setBtnWith2Address(UartScreenVar.addr_icon_parameter_enabled, new byte[] {0x00});
        }
        else if (key_value == 0x03) {
            detectionEnabled = !detectionEnabled;
            if (detectionEnabled)
                writeText(UartScreenVar.addr_icon_detection_enabled, new byte[] {0x00, 67});
            else
                writeText(UartScreenVar.addr_icon_detection_enabled, new byte[] {0x00, 66});
        }
        else if (key_value >= 0x06 && key_value <=0x0f){
        	action_parameters_set(payload);
        }
        last_uiID = cmd;
    }

    private void action_parameters_set(byte[] payload)
    {
        if (payload.length < 9)
            return;

        if (getPrinter().getStatus().isPrintInProgress())
            return;

        char cmd = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));

//        int value = ((payload[7] & 0xFF) << 8) + (payload[8] & 0xFF);
        String strValue = new String(BasicUtillities.subBytes(payload,8,payload.length-8-5)); 
        int value = Integer.valueOf(strValue);
        

        if (cmd == UartScreenVar.addr_txt_parameters[0][1])
            numberOfFirstLayers = value;
        else if (cmd == UartScreenVar.addr_txt_parameters[1][1])
            firstLayerTime = value;
        else if (cmd == UartScreenVar.addr_txt_parameters[2][1])
            layerTime = value;
        else if (cmd == UartScreenVar.addr_txt_parameters[3][1])
            resumeLayerTime = value;
        else if (cmd == UartScreenVar.addr_txt_parameters[4][1])
            liftDistance = value/10.0;
        else if (cmd == UartScreenVar.addr_txt_parameters[5][1])
            liftFeedSpeed = value/10.0;
        else if (cmd == UartScreenVar.addr_txt_parameters[6][1])
            liftRetractSpeed = value/10.0;
        else if (cmd == UartScreenVar.addr_txt_parameters[7][1])
            delayTimeBeforeSolidify = value;
        else if (cmd == UartScreenVar.addr_txt_parameters[8][1])
            delayTimeAfterSolidify = value;
        else if (cmd == UartScreenVar.addr_txt_parameters[9][1])
            delayTimeAsLiftedTop = value;
//        else if (cmd == UartScreenVar.addr_txt_parameters[10][1])
//            delayTimeForAirPump = value;
    }

    private void action_language(byte[] payload)
    {
        if (payload.length < 9)
            return;

        int key_value = 0;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));

        ParameterRecord parameterRecord = HostProperties.Instance().getParameterRecord();
        if (key_value == 6)	//set to CN
            parameterRecord.setLanguage(0);  
        else if (key_value == 7) //set to EN
        	parameterRecord.setLanguage(1);
//        else if (key_value == )	//TBD
//            parameterRecord.setLanguage(3);
        else
        	return;
        HostProperties.Instance().saveParameterRecord(parameterRecord);
        setMachineStatus(getPrinter().getStatus(), true, false);
        goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.Main));
    }

    private void action_move_control(byte[] payload)
    {
        if (payload.length < 9)
            return;

        if (getPrinter().getStatus().isPrintInProgress())
            return;

        int key_value = 0;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));
//        key_value = payload[8];

        if (key_value == 8) {
            //Z鏉炵繝绗傜粔锟�
            getPrinter().getGCodeControl().executeSetRelativePositioning();
            getPrinter().getGCodeControl().sendGcode("G1 Z1 F1000");
            getPrinter().getGCodeControl().executeSetAbsolutePositioning();
        }
        else if (key_value == 10) {
            //Z鏉炵繝绗呯粔锟�
            getPrinter().getGCodeControl().executeSetRelativePositioning();
            getPrinter().getGCodeControl().sendGcode("G1 Z-1 F1000");
            getPrinter().getGCodeControl().executeSetAbsolutePositioning();
        }
        else if (key_value == 9) {
            //Z鏉炴潙缍婇梿锟�
            getPrinter().getGCodeControl().executeZHome();
        }
        else if (key_value == 6) {
            //Z鏉炵繝绗傜粔璇插煂妞ゅ爼鍎�
            getPrinter().getGCodeControl().executeSetAbsolutePositioning();
            int zTravel = getPrinter().getConfiguration().getSlicingProfile().getZTravel();
            String gCode = String.format("G1 Z%d F1000", zTravel);
            getPrinter().getGCodeControl().sendGcode(gCode);
        }
        else if (key_value == 7) {
            //Z鏉炵繝绗呯粔璇插煂鎼存洟鍎�
            getPrinter().getGCodeControl().executeSetAbsolutePositioning();
            getPrinter().getGCodeControl().sendGcode("G1 Z0 F1000");
        }

    }

    private void action_optical_control(byte[] payload)
    {
        if (payload.length < 9)
            return;

        if (getPrinter().getStatus().isPrintInProgress())
            return;

        int key_value = 0;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));
//        key_value = payload[8];

        if (key_value == 0x00) {
            //initial screen
            double temperature = 0;
            String receive = getPrinter().getGCodeControl().executeQueryTemperature();
            Pattern GCODE_Temperature_PATTERN = Pattern.compile("\\s*T:\\s*(-?[\\d\\.]+).*B:(-?[\\d\\.]+).*");
            Matcher matcher = GCODE_Temperature_PATTERN.matcher(receive);
            if (matcher.find()) {
                temperature = Double.parseDouble(matcher.group(2));
            }
            writeTextWith2Address(UartScreenVar.addr_txt_led_temperature, String.format("%-16s", String.format("%.1f", temperature)).getBytes());
        } else if (key_value == 7) {
            //led on/off
            if (!ledBoardEnabled) {
                getPrinter().getGCodeControl().executeShutterOn();
                ledBoardEnabled = true;
                if (shutterTimer != null) {
                    shutterTimer.cancel();
                    shutterTimer = null;
                }
                shutterTimer = new Timer();
                shutterTimer.schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        getPrinter().getGCodeControl().executeShutterOff();
                        /////modified by derby 3-19, close the ledboard, and Ledcooler waterpump
                        getPrinter().getGCodeControl().executeLedCoolerOff();
                        getPrinter().getGCodeControl().executeWaterPumpOff();
                        waterPumpEnabled = false;
                        ledBoardEnabled = false;
                        setBtnWith2Address(UartScreenVar.addr_icon_led_board, new byte[]{0x00});
                    }
                }, 40000);
            } else {
                if (shutterTimer != null) {
                    shutterTimer.cancel();
                    shutterTimer = null;
                }
                getPrinter().getGCodeControl().executeShutterOff();
                ledBoardEnabled = false;
            }
        } else if (key_value == 8) {
            //濮樻潙鍠庡锟介崗锟�
            if (!waterPumpEnabled) {
                getPrinter().getGCodeControl().executeWaterPumpOn();
                waterPumpEnabled = true;
            } else {
                getPrinter().getGCodeControl().executeWaterPumpOff();
                waterPumpEnabled = false;
            }
        } else if (key_value == 9) {
            //妫板嫮鐤嗛崶鎯у剼
            if (!imageLogoEnabled) {
                showImage("/opt/cwh/3DTALK.png");
                imageLogoEnabled = true;
                imageFullEnabled = false;
            } else {
                showImage(null);
                imageLogoEnabled = false;
            }
        } else if (key_value == 10) {
            //閸忋劌鐫嗛惂鍊熷
            if (!imageFullEnabled) {
                showImage("/opt/cwh/WHITE.png");
                imageFullEnabled = true;
                imageLogoEnabled = false;
            } else {
                showImage(null);
                imageFullEnabled = false;
            }
        } else {
            //闁拷閸戞椽銆夐棃锟�
            if (shutterTimer != null) {
                shutterTimer.cancel();
                shutterTimer = null;
            }
            getPrinter().getGCodeControl().executeShutterOff();
            ledBoardEnabled = false;

            showImage(null);
            imageFullEnabled = false;
            imageLogoEnabled = false;

            if (key_value == 0x05) {
                filesUpdate("local", 0);
            } else if (key_value == 0x07) {
                action_about();
            }
        }

        if (ledBoardEnabled)
        	setBtnWith2Address(UartScreenVar.addr_icon_led_board, new byte[]{0x01});
        else
        	setBtnWith2Address(UartScreenVar.addr_icon_led_board, new byte[]{0x00});
        if (waterPumpEnabled)
        	setBtnWith2Address(UartScreenVar.addr_icon_water_pump, new byte[]{0x01});
        else
        	setBtnWith2Address(UartScreenVar.addr_icon_water_pump, new byte[]{0x00});
        if (imageLogoEnabled)
        	setBtnWith2Address(UartScreenVar.addr_icon_image_logo, new byte[]{0x01});
        else
        	setBtnWith2Address(UartScreenVar.addr_icon_image_logo, new byte[]{0x00});
        if (imageFullEnabled)
        	setBtnWith2Address(UartScreenVar.addr_icon_image_full, new byte[]{0x01});
        else
        	setBtnWith2Address(UartScreenVar.addr_icon_image_full, new byte[]{0x00});

    }

    private void action_set_admin_password(byte[] payload)
    {
    	///modify by derby2021-1-4. 
    	int key_value = 0;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));
        else if(payload[2] == 0x01)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 14, 2));
    	if(key_value != 6)
    		return;
        String password = new String(BasicUtillities.subBytes(payload,8,payload.length-4)); 
        loadAdminAccount(password.replaceAll("[^\\x21-\\x7E]", ""));
    }

    private void action_replace_part(byte[] payload)
    {
        if (payload.length < 9)
            return;
        
        char cmd;
        cmd = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 3, 2));

    	int key_value = 0;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));

        if (key_value == 10) {
            getPrinter().setLedUsedTime(0);
        }
        else if (key_value == 11) {
            getPrinter().setScreenUsedTime(0);
        }
        if (cmd == UartScreenVar.addr_btn_info_success && key_value == 2)
        	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.replacement));
        setLiftTime();
        last_uiID = cmd;
    }

    private void action_led_pwm_adjust(byte[] payload)
    {
    	char cmd;
        cmd = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 3, 2));

    	int key_value = 0;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));

        if (key_value == 0x00) {
            //initial screen
            ledPwmValue = new Integer(getPrinter().getGCodeControl().executeReadLedPwmValue());
            writeTextWith2Address(UartScreenVar.addr_txt_led_pwm, Integer.toString(ledPwmValue).getBytes());
            writeTextWith2Address(UartScreenVar.addr_txt_pwm_slider, BasicUtillities.intToBytes(ledPwmValue));
//            writeText(UartScreenVar.addr_txt_led_pwm, new byte[]{(byte) ((ledPwmValue >> 8) & 0xFF), (byte) (ledPwmValue & 0xFF)});
        }
        else if(key_value == 10) {
        	ledPwmValue = payload[11];
        }
        else if (key_value == 6) {
            //led on
            if (!ledBoardEnabled) {
                getPrinter().getGCodeControl().executeShutterOn();
                ledBoardEnabled = true;
                if (shutterTimer != null) {
                    shutterTimer.cancel();
                    shutterTimer = null;
                }
                shutterTimer = new Timer();
                shutterTimer.schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        getPrinter().getGCodeControl().executeShutterOff();
                        ledBoardEnabled = false;
                        setBtnWith2Address(UartScreenVar.addr_icon_led_board, new byte[]{0x00});
                    }
                }, 40000);
            } else {
                if (shutterTimer != null) {
                    shutterTimer.cancel();
                    shutterTimer = null;
                }
                getPrinter().getGCodeControl().executeShutterOff();
                ledBoardEnabled = false;
            }
        } else if (key_value == 7) //save pwm to control board
        {
            getPrinter().getGCodeControl().executeWriteLedPwmValue(ledPwmValue);
            goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.operation_success));
            
        }
//            else if (cmd == UartScreenVar.addr_btn_info_success && key_value == 2) {
//        	goPage(last_uiID);
//        }
        else //other button
        {
            if (shutterTimer != null) {
                shutterTimer.cancel();
                shutterTimer = null;
            }
            getPrinter().getGCodeControl().executeShutterOff();
            ledBoardEnabled = false;

            if (key_value == 0x04) {
                filesUpdate("local", 0);
            } else if (key_value == 0x05) {
                action_about();
            }
        }

        if (ledBoardEnabled)
        	setBtnWith2Address(UartScreenVar.addr_icon_led_board, new byte[]{0x01});
        else
        	setBtnWith2Address(UartScreenVar.addr_icon_led_board, new byte[]{0x00});
        last_uiID = cmd;
    }

    private void action_clear_trough(byte[] payload)
    {
        if (payload.length < 9 || getPrinter().isPrintInProgress())
            return;
        char cmd;
        cmd = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 3, 2));
        int key_value = 0;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));

        if (key_value == 2 && cmd == UartScreenVar.addr_btn_clear_trough) {
        	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.Resin_clean));
            getPrinter().getGCodeControl().executeShutterOn();
            showImage("/opt/cwh/WHITE.png");
            if (shutterTimer != null) {
                shutterTimer.cancel();
                shutterTimer = null;
            }
            shutterTimer = new Timer();
            shutterTimer.schedule(new TimerTask() {
                @Override
                public void run()
                {
                	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.Settings));
                    getPrinter().getGCodeControl().executeShutterOff();
                    showImage(null);
                }
            }, 10000);
        }
    }

    private void action_set_led_pwm(byte[] payload)
    {
        if (payload.length < 9)
            return;

        if (getPrinter().getStatus().isPrintInProgress())
            return;

        ledPwmValue = ((payload[7] & 0xFF) << 8) + (payload[8] & 0xFF);
    }

    private void action_about()
    {
        setVersion(version, UartScreenVar.addr_txt_software_version);
        setVersion(getPrinter().getGCodeControl().executeGetFirmwareVersion(), UartScreenVar.addr_txt_hardware_version);
        setVersion(getPrinter().getGCodeControl().getSerialNumber(), UartScreenVar.addr_txt_serialNumber);
        String ipAddress = getIpAddress();
        if (ipAddress != null) {
        	writeTextWith2Address(UartScreenVar.addr_txt_ipAddress, String.format("%-16s", ipAddress).getBytes());
        }
        else {
        	writeTextWith2Address(UartScreenVar.addr_txt_ipAddress, String.format("%-16s", "").getBytes());
        }
        //String modelNumber = HostProperties.Instance().getModelNumber();
        writeTextWith2Address(UartScreenVar.addr_txt_modelNumber, String.format("%-16s", "DS280HD").getBytes());
    }

    private void action_update_software(byte[] payload)
    {
        if (payload.length < 9)
            return;

        if (getPrinter().getStatus().isPrintInProgress())
            return;

//        int value = payload[8];
        char cmd;
        cmd = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 3, 2));
        int key_value = 0;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));

        if (key_value == 2 ) {
            Main.GLOBAL_EXECUTOR.submit(new Runnable() {
                @Override
                public void run()
                {
                	System.out.println("entering update softwware..");
                    if (check_updatable() && HostProperties.Instance().isEnableUpdate()) {
                        close();
                        goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.Updating));
                        start_update();
                        goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.Updated));
                    }
                    else {
                    	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.update_software_failed));
                    }
                }
            });
        }
        if (key_value == 2 && cmd == UartScreenVar.addr_btn_update_software_failed)
        	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.Settings));
        last_uiID = cmd;
    }

    private void action_update_firmware(byte[] payload)
    {
        if (payload.length < 9)
            return;

        if (getPrinter().getStatus().isPrintInProgress())
            return;

        char cmd;
        cmd = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 3, 2));
        int key_value = 0;
        if(payload[2] == 0x11)
        	key_value = BasicUtillities.byteArrayToChar(BasicUtillities.subBytes(payload, 5, 2));

        try {
            if (key_value == 2) {
                String filename = check_firmware_updatable();
                if (filename == null) {
                	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.update_software_failed));
                    return;
                }
                goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.Updating));
                ///////modified by derby 6-30 firmware update conflict with oqton collector service ,stop it temporary 
                IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "sudo service oqton.data.collector stop"}, null);
                Thread.sleep(500);
                FirmwareInstall firmwareInstall = new FirmwareInstall(getPrinter());
                if (firmwareInstall.runInstall(filename)) {
//                    writeKey((short)120,(short)120);   //modified by derby for new dwin screen
//                    Thread.sleep(500);
//                    writeKey((short)16,(short)16);
                	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.Updated));
                }
                else {
//                    writeKey((short)16,(short)16);
//                    Thread.sleep(500);
//                    writeKey((short)66,(short)16);
                	goPage(UartScreenVar.getPagePos(getLanguage(), getModelNumber(), UartScreenVar.PagePos.update_firmware_failed));
                }
                IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "sudo service oqton.data.collector start"}, null);
            }
        }
        catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }

    /***************************action function end**************************************/

    private void start_update()
    {
        try {
            System.out.println("update started");
            getPrinter().getUartScreenSerialPort().write(new byte[]{ (byte)0xEE, (byte)0xB1,0x00,0x00, 0x22, (byte)0xFF, (byte)0xFC, (byte)0xFF, (byte)0xFF});
            Thread.sleep(100);
            update_dgus();
            update_filesystem();
            getPrinter().getUartScreenSerialPort().write(new byte[]{(byte)0xEE, (byte)0xB1,0x00,0x00, 0x1F, (byte)0xFF, (byte)0xFC, (byte)0xFF, (byte)0xFF});
            System.out.println("update completed");
            while (BasicUtillities.isExists(update_path)) {
                Thread.sleep(1000);
            }
            getPrinter().getUartScreenSerialPort().write(new byte[]{(byte)0xEE, (byte)0xB1,0x00,0x00, 0x06, (byte)0xFF, (byte)0xFC, (byte)0xFF, (byte)0xFF});
            Thread.sleep(100);
            IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "sudo /etc/init.d/cwhservice restart"}, null);
        }
        catch (IOException | InterruptedException e) {
            System.out.println(e.toString());
        }
    }

    private boolean check_updatable()
    {
        String dgus_path = update_path + "/DWIN_SET";
        String filesystem_path = update_path + "/filesystem";
        String version_path = update_path + "/version";

        try {
            if (!BasicUtillities.isExists(dgus_path) ||
                    !BasicUtillities.isExists(filesystem_path) ||
                    !BasicUtillities.isExists(version_path)) {
            	System.out.println("no update files found!");
            	return false;
            }
                

            String version_string = BasicUtillities.readAll(version_path);
            String old_version = version.replace(".", "");
            String new_version = version_string.replace(".", "");
            if (Integer.parseInt(old_version) >= Integer.parseInt(new_version)) {
            	System.out.println("ver old!");
            	return false;
            }
                
        }
        catch (Exception e) {
            System.out.println(e.toString());
            return false;
        }

        return true;
    }

    private String check_firmware_updatable()
    {
        String firmware_path = update_path + "/firmware";
//        String firmware_path = "D:\\Users\\zyd\\Desktop";

        File[] files = new File(firmware_path).listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".hex") ||
                        file.getName().endsWith(".HEX")) {
                    return file.getAbsolutePath();
                }
            }
        }
        return null;
    }

    private void update_filesystem()
    {
        String updateScript = update_path + "/update.sh";
        if (BasicUtillities.isExists(updateScript)) {
            IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "sudo " + updateScript}, null);
        }
    }

    private void update_dgus()
    {
        String dwinPath = update_path + "/DWIN_SET";

        File[] files = new File(dwinPath).listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".icl")) {
                System.out.println("update dgus icl");
                update_dgus_icl(file);
            }
            if (file.getName().toLowerCase().endsWith(".bin") ||
                    file.getName().toLowerCase().endsWith(".CFG") ||
                    file.getName().toLowerCase().endsWith(".hkz")) {
                System.out.println("update dgus others");
                //update_dgus_others(file);
            }
        }
    }
    
    private boolean update_dgus_icl(File file)
    {
    	int byteRead = 0;
    	int icl_number;
        byte[] icl_block;
        byte[] buff;
        int addr = 0x8000;
        InputStream inputStream = null;

        String filename = file.getName();
//        if (filename.toLowerCase().endsWith(".icl")) {
            try {
                icl_number = Integer.parseInt(filename.substring(0, filename.lastIndexOf(".")));
                inputStream = new FileInputStream(file);
                icl_block = new byte[32*1024];
                int write_addr = icl_number*256/32;
                while(true) {
                	byteRead = inputStream.read(icl_block);
                	if(byteRead == -1)
                		break;
	                int i = 0;
	               do {
	                	buff = BasicUtillities.subBytes(icl_block,240*i,240);
	                	
	                    byte[] bytes = {0x5A,(byte)0xA5,(byte)0xF3,(byte)0x82,(byte)(addr>>8),(byte)addr};
	                    bytes = BasicUtillities.bytesCat(bytes, buff);
	                	getPrinter().getUartScreenSerialPort().write(bytes);
	                	addr = addr + 0x78;
	                	i++;
//	                	if(i==136)
//	                		System.out.print(addr);
	                }
	               	while(i<(byteRead/240));
	               
	               	byte[] bytes = {0x5A,(byte)0xA5,(byte)((byteRead-240*i)+3),(byte)0x82,(byte)(addr>>8),(byte)addr};
	               	buff = BasicUtillities.subBytes(icl_block, 240*i, byteRead-240*i);
                   	bytes = BasicUtillities.bytesCat(bytes, buff);
               		getPrinter().getUartScreenSerialPort().write(bytes);
	               
	                getPrinter().getUartScreenSerialPort().write(new byte[] {0x5A,(byte)0xA5,0x0F,(byte)0X82,0X00,(byte)0XAA,0X5A,0X02,(byte)(write_addr>>8),(byte)write_addr,(byte)0x80,0x00,0x00,0x14,0x00,0x00,0x00,0x00});
	                write_addr++;
	                Thread.sleep(200);

                }
            }
            catch (Exception e) {
				// TODO: handle exception
			}
            finally {
            	try {
                    if (inputStream != null)
                        inputStream.close();
                }
                catch (IOException e) {
                    System.out.println(e.toString());
                }
			}
		
//        }
        return true;
    }

    private boolean update_dgus_bmp(File file)
    {
        int byteRead;
        byte[] bmp_head;
        byte[] bmp_body;
        byte[] img5r6b6g;
        int bmp_width, bmp_height;
        int b, g, r;
        byte[] receive;
        int bmp_number;
        InputStream inputStream = null;

        String filename = file.getName();
        if (filename.toLowerCase().endsWith(".bmp")) {
            try {
                bmp_number = Integer.parseInt(filename.replace(".bmp", ""));
                inputStream = new FileInputStream(file);
                bmp_head = new byte[54];
                byteRead = inputStream.read(bmp_head);
                if (byteRead != 54)
                    return false;
                bmp_width = (((bmp_head[21]) & 0xFF) << 24) + (((bmp_head[20]) & 0xFF) << 16) + (((bmp_head[19]) & 0xFF) << 8) + ((bmp_head[18]) & 0xFF);
                bmp_height = (((bmp_head[25]) & 0xFF) << 24) + (((bmp_head[24]) & 0xFF) << 16) + (((bmp_head[23]) & 0xFF) << 8) + ((bmp_head[22]) & 0xFF);
                bmp_body = new byte[bmp_width*bmp_height*3];
                byteRead = inputStream.read(bmp_body);
                if (byteRead != bmp_width*bmp_height*3)
                    return false;
                img5r6b6g = new byte[bmp_width*bmp_height*2];
                for (int i = 0; i < bmp_width*bmp_height; i++) {
                    b = (bmp_body[3*i] & 0xF8);
                    g = (bmp_body[3*i + 1] & 0xFC);
                    r = (bmp_body[3*i + 2] & 0xF8);
                    img5r6b6g[2*i] = (byte) ((r + (g >> 5)) & 0xff);
                    img5r6b6g[2*i + 1] = (byte) (((g << 3) + (b >> 3)) & 0xff);
                }

                getPrinter().getUartScreenSerialPort().write(new byte[]{0x5A,(byte)0xA5,0x06,(byte)0x80,(byte)0xF5,0x5A,0x00,0x00,(byte) bmp_number});
                receive = IOUtilities.read(getPrinter().getUartScreenSerialPort(), 2000, 10);
                if (!"OK".equals(new String(receive)))
                    return false;
                for (int i = bmp_height - 1; i >= 0; i--) {
                    getPrinter().getUartScreenSerialPort().write(BasicUtillities.subBytes(img5r6b6g, bmp_width*2*i, bmp_width*2));
                }
                Thread.sleep(1000);
                getPrinter().getUartScreenSerialPort().write(new byte[]{0x5A,(byte)0xA5,0x04,(byte)0x80,0x03,0x00,(byte) bmp_number});
                Thread.sleep(100);
            }
            catch (IOException | InterruptedException e) {
                System.out.println(e.toString());
            }
            finally {
                try {
                    if (inputStream != null)
                        inputStream.close();
                }
                catch (IOException e) {
                    System.out.println(e.toString());
                }
            }
        }
        else {
            return false;
        }
        return true;
    }

    private boolean update_dgus_others(File file)
    {
        InputStream inputStream = null;
        int fileNumber;
        byte[] dgusCommand;
        byte[] fileData;
        byte[] receive;
        int byteRead;

        String filename = file.getName();
        try {
            fileNumber = Integer.parseInt(filename.substring(0, filename.lastIndexOf(".")));

            inputStream = new FileInputStream(file);
            fileData = new byte[256*1024];
            while ((byteRead = inputStream.read(fileData)) != -1) {
                dgusCommand = new byte[]{0x5A,(byte)0xA5,0x04,(byte)0x80,(byte)0xF3,0x5A,(byte)fileNumber};
                getPrinter().getUartScreenSerialPort().write(dgusCommand);
                receive = IOUtilities.read(getPrinter().getUartScreenSerialPort(), 2000, 10);
                if (!"OK".equals(new String(receive)))
                    return false;
                getPrinter().getUartScreenSerialPort().write(BasicUtillities.subBytes(fileData, 0, byteRead));
                fileNumber++;
                Thread.sleep(1000);
            }

        }
        catch (IOException | InterruptedException e) {
            System.out.println(e.toString());
        }
        finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            }
            catch (IOException e) {
                System.out.println(e.toString());
            }
        }
        return true;
    }

}
