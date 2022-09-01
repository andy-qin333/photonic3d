package org.area515.resinprinter.uartscreen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
import org.area515.resinprinter.printer.ParameterRecord;
import org.area515.resinprinter.printer.Printer;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.server.Main;
import org.area515.resinprinter.services.MachineService;
import org.area515.resinprinter.services.PrintableService;
import org.area515.resinprinter.services.PrinterService;
import org.area515.util.BasicUtillities;
import org.area515.util.IOUtilities;

import com.alibaba.fastjson.JSONObject;


public class UartScreenNet
{
    private String version = "0.5.10";  //derby on 2020-10-14 for ds300

    private Printer printer;
    
    private String update_path = "/udiskdir/update-dlp";
    
	private Timer shutterTimer;
	
    private boolean ledBoardEnabled = false;
    private boolean waterPumpEnabled = false;
    private boolean imageLogoEnabled = false;
    private boolean imageFullEnabled = false;
    
    private int ledPwmValue = 0;
    
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

    public UartScreenNet(Printer printer)
    {
        this.printer = printer;
    }
    
    public Printer getPrinter()
    {
        return this.printer;
    }
    
    public void action_clear_trough(int value)
    {
        
        if (value == 1) {
           
            getPrinter().getGCodeControl().executeShutterOn();
            getPrinter().showImage("/opt/cwh/WHITE.png");
            if (shutterTimer != null) {
                shutterTimer.cancel();
                shutterTimer = null;
            }
            shutterTimer = new Timer();
            shutterTimer.schedule(new TimerTask() {
                @Override
                public void run()
                {
                   getPrinter().getGCodeControl().executeShutterOff();
                   getPrinter().showImage("");
                }
            }, 10000);
            getPrinter().setCntLayersClearResin(0);
        }
    }
    
    public void action_optical_control(String value)
	{

        if (getPrinter().getStatus().isPrintInProgress())
            return;

        int key_value;
        key_value = Integer.parseInt(value);

        if (key_value == 0x00) {
            //鏉╂稑鍙嗛幒褍鍩楁い锟�
            double temperature = 0;
            String receive = getPrinter().getGCodeControl().executeQueryTemperature();
            Pattern GCODE_Temperature_PATTERN = Pattern.compile("\\s*T:\\s*(-?[\\d\\.]+).*B:(-?[\\d\\.]+).*");
            Matcher matcher = GCODE_Temperature_PATTERN.matcher(receive);
            if (matcher.find()) {
                temperature = Double.parseDouble(matcher.group(2));
            }
          //  writeText(UartScreenVar.addr_txt_led_temperature, String.format("%-16s", String.format("%.1f", temperature)).getBytes());
        } else if (key_value == 0x01) {
            //閻忣垱婢樺锟介崗锟�
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
                        //writeText(UartScreenVar.addr_icon_led_board, new byte[]{0x00, 72});
                    }
                }, 40000);
            } else {
                if (shutterTimer != null) {
                    shutterTimer.cancel();
                    shutterTimer = null;
                }
                getPrinter().getGCodeControl().executeShutterOff();
                /////modified by derby 3-19, close the ledboard, and Ledcooler waterpump
                getPrinter().getGCodeControl().executeLedCoolerOff();
                getPrinter().getGCodeControl().executeWaterPumpOff();
                waterPumpEnabled = false;
                ledBoardEnabled = false;
            }
        } else if (key_value == 0x02) {
            //濮樻潙鍠庡锟介崗锟�
            if (!waterPumpEnabled) {
            	getPrinter().getGCodeControl().executeWaterPumpOn();
                waterPumpEnabled = true;
            } else {
            	getPrinter().getGCodeControl().executeWaterPumpOff();
                waterPumpEnabled = false;
            }
        } else if (key_value == 0x03) {
            //妫板嫮鐤嗛崶鎯у剼
            if (!imageLogoEnabled) {
            	getPrinter().showImage("/opt/cwh/3DTALK.png");
                imageLogoEnabled = true;
                imageFullEnabled = false;
            } else {
            	getPrinter().showImage("");
                imageLogoEnabled = false;
            }
        } else if (key_value == 0x04) {
            //閸忋劌鐫嗛惂鍊熷
            if (!imageFullEnabled) {
            	getPrinter().showImage("/opt/cwh/WHITE.png");
                imageFullEnabled = true;
                imageLogoEnabled = false;
            } else {
            	getPrinter().showImage("");
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

            getPrinter().showImage("");
            imageFullEnabled = false;
            imageLogoEnabled = false;

            if (key_value == 0x05) {
                //filesUpdate("local", 0);
            } else if (key_value == 0x07) {
               // action_about();
            }
        }
/*
        if (ledBoardEnabled)
            writeText(UartScreenVar.addr_icon_led_board, new byte[]{0x00, (byte) UartScreenVar.getIconPos(getLanguage(), getModelNumber(), UartScreenVar.IconPos.LightSwitch)});
        else
            writeText(UartScreenVar.addr_icon_led_board, new byte[]{0x00, (byte) UartScreenVar.getIconPos(getLanguage(), getModelNumber(), UartScreenVar.IconPos.Empty1)});
        if (waterPumpEnabled)
            writeText(UartScreenVar.addr_icon_water_pump, new byte[]{0x00, (byte) UartScreenVar.getIconPos(getLanguage(), getModelNumber(), UartScreenVar.IconPos.WaterSwitch)});
        else
            writeText(UartScreenVar.addr_icon_water_pump, new byte[]{0x00, (byte) UartScreenVar.getIconPos(getLanguage(), getModelNumber(), UartScreenVar.IconPos.Empty1)});
        if (imageLogoEnabled)
            writeText(UartScreenVar.addr_icon_image_logo, new byte[]{0x00, (byte) UartScreenVar.getIconPos(getLanguage(), getModelNumber(), UartScreenVar.IconPos.PresetImage)});
        else
            writeText(UartScreenVar.addr_icon_image_logo, new byte[]{0x00, (byte) UartScreenVar.getIconPos(getLanguage(), getModelNumber(), UartScreenVar.IconPos.Empty1)});
        if (imageFullEnabled)
            writeText(UartScreenVar.addr_icon_image_full, new byte[]{0x00, (byte) UartScreenVar.getIconPos(getLanguage(), getModelNumber(), UartScreenVar.IconPos.FullScreenImage)});
        else
            writeText(UartScreenVar.addr_icon_image_full, new byte[]{0x00, (byte) UartScreenVar.getIconPos(getLanguage(), getModelNumber(), UartScreenVar.IconPos.Empty1)});
*/
	}
    
    public void action_preset_image(String imagePath)
	{
		getPrinter().showImage(imagePath);
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
    
    public boolean action_update_firmware(int value)
    {
        try {
            if (value == 1) {
                String filename = check_firmware_updatable();
                if (filename == null) {
                   // writeKey((byte)0xF2);
                    return false;
                }
               // writeKey((byte)0xF3);
                Thread.sleep(500);
                FirmwareInstall firmwareInstall = new FirmwareInstall(getPrinter());
                if (firmwareInstall.runInstall(filename)) {
                 //   writeKey((byte)0xF1);
                    Thread.sleep(500);
                 //   writeKey((byte)0xF1);
                }
                else {
                 //   writeKey((byte)0xF1);
                    Thread.sleep(500);
                //    writeKey((byte) 0xF2);
                }
            }
        }
        catch (InterruptedException e) {
            System.out.println(e.toString());
            return false;
        }
        
        return true;
    }

    public String action_led_pwm_adjust(int key_value)
    {
        if (key_value == 0x00) {
            //杩涘叆鐏澘鏍″噯
            ledPwmValue = new Integer(getPrinter().getGCodeControl().executeReadLedPwmValue());
           // writeText(UartScreenVar.addr_txt_led_pwm, new byte[]{(byte) ((ledPwmValue >> 8) & 0xFF), (byte) (ledPwmValue & 0xFF)});
            return Integer.toString(ledPwmValue);
        } else if (key_value == 0x01) {
            //鐏澘寮�鍏�
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
                      //  writeText(UartScreenVar.addr_icon_led_board, new byte[]{0x00, (byte) UartScreenVar.getIconPos(getLanguage(), getModelNumber(), UartScreenVar.IconPos.Empty1)});
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
        } else if (key_value == 0x02) //娣囨繂鐡ㄩ悘顖涙緲瀵搫瀹�
        {
            getPrinter().getGCodeControl().executeWriteLedPwmValue(ledPwmValue);
           // writeKey((byte) 0xF1);
        } else //闁拷閸戣櫣鏅棃锟�
        {
            if (shutterTimer != null) {
                shutterTimer.cancel();
                shutterTimer = null;
            }
            getPrinter().getGCodeControl().executeShutterOff();
            ledBoardEnabled = false;

            if (key_value == 0x04) {
               // filesUpdate("local", 0);
            } else if (key_value == 0x05) {
               // action_about();
            }
        }
/*
        if (ledBoardEnabled)
            writeText(UartScreenVar.addr_icon_led_board, new byte[]{0x00, (byte) UartScreenVar.getIconPos(getLanguage(), getModelNumber(), UartScreenVar.IconPos.LightSwitch)});
        else
            writeText(UartScreenVar.addr_icon_led_board, new byte[]{0x00, (byte) UartScreenVar.getIconPos(getLanguage(), getModelNumber(), UartScreenVar.IconPos.Empty1)});
*/
        return "ok";
    }
    
    public void action_set_led_pwm(int value)
    {

        if (getPrinter().getStatus().isPrintInProgress())
            return;

        ledPwmValue = value;
        getPrinter().getGCodeControl().executeWriteLedPwmValue(ledPwmValue);
        
    }
    
    public void action_replace_part(int key_value)
    {
     
        if (key_value == 1) {
            getPrinter().setLedUsedTime(0);
            //writeKey((byte)0xF1);
        }
        else if (key_value == 2) {
            getPrinter().setScreenUsedTime(0);
            //writeKey((byte)0xF1);
        }
        else if (key_value == 3) {
            getPrinter().setCntLayersReplacefilm(0);
          //  writeKey((byte)0xF1);
            getPrinter().setCntLayersReplacefilm(0);
            getPrinter().setCntReplaceFilm(getPrinter().getCntReplaceFilm()+1);   //记录更换离型膜次数
        }
        setLiftTime();
    }
    
    private void setLiftTime()
    {
        String string;
        int iconNum;

        long ledUsedTime = getPrinter().getLedUsedTime();
        string = String.format("%.1f/%d", ledUsedTime/(60*60*1000.0), 5000);
        iconNum = (int)(102 - ledUsedTime / (60*60*1000.0*1000));
       // writeText(UartScreenVar.addr_txt_lifetime_led, String.format("%-10s", string).getBytes());
       // writeText(UartScreenVar.addr_icon_lifetime_led, new byte[] {0x00, (byte)(iconNum < 97?97:iconNum)}); //add by derby 2020/1/14 led_life icon

        long screenUsedTime = getPrinter().getScreenUsedTime();
        string = String.format("%.1f/%d", screenUsedTime/(60*60*1000.0), 1000);
        iconNum = (int)(102 - screenUsedTime / (60*60*1000.0*200));
       // writeText(UartScreenVar.addr_txt_lifetime_screen, String.format("%-10s", string).getBytes());
       // writeText(UartScreenVar.addr_icon_lifetime_screen, new byte[] {0x00, (byte)(iconNum < 97?97:iconNum)}); //add by derby 2020/1/14 screen_life icon
        
        int filmUsedTime = getPrinter().getCntLayersReplacefilm();
        string = String.format("%.1f/%d", filmUsedTime*1.0, HostProperties.Instance().getLayersReplaceFilm());
        iconNum = (int)(102 - filmUsedTime*5 / HostProperties.Instance().getLayersReplaceFilm());
       // writeText(UartScreenVar.addr_txt_lifetime_film, String.format("%-10s", string).getBytes());
      //  writeText(UartScreenVar.addr_icon_lifetime_film, new byte[] {0x00, (byte)(iconNum < 97?97:iconNum)}); //add by derby 2020/1/14 led_life icon

    }
    
    public  String action_get_lift_time() {
		String json = "{";
		
		String string;
      

        long ledUsedTime = getPrinter().getLedUsedTime();
        string = String.format("%.1f/%d", ledUsedTime/(60*60*1000.0), 5000);
        json += "\"ledUsedTime\":" + "\"" + string + "\"," ;
     
        long screenUsedTime = getPrinter().getScreenUsedTime();
        string = String.format("%.1f/%d", screenUsedTime/(60*60*1000.0), 1000);
        json += "\"screenUsedTime\":" + "\""  + string + "\",";
    
        int filmUsedTime = getPrinter().getCntLayersReplacefilm();
        string = String.format("%d/%d", filmUsedTime, HostProperties.Instance().getLayersReplaceFilm());
        json += "\"filmUsedTime\":" + "\""  + string + "\"}";
    
		return json;
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
    
    public void action_save_parameters(String json)
    {
    	JSONObject object = JSONObject.parseObject(json);
    	
    	String valueString = object.getString(String.valueOf((int)UartScreenVar.addr_txt_parameters[0]));
    	numberOfFirstLayers = Integer.parseInt(valueString);
    	
    	valueString = object.getString(String.valueOf((int)UartScreenVar.addr_txt_parameters[1]));
    	firstLayerTime = Integer.parseInt(valueString);    	
    	
    	valueString = object.getString(String.valueOf((int)UartScreenVar.addr_txt_parameters[2]));
    	layerTime = Integer.parseInt(valueString);   
    	
    	valueString = object.getString(String.valueOf((int)UartScreenVar.addr_txt_parameters[3]));
    	resumeLayerTime = Integer.parseInt(valueString);   
    	
    	valueString = object.getString(String.valueOf((int)UartScreenVar.addr_txt_parameters[4]));
    	liftDistance = Integer.parseInt(valueString); 
    	
    	valueString = object.getString(String.valueOf((int)UartScreenVar.addr_txt_parameters[5]));
    	liftFeedSpeed = Integer.parseInt(valueString); 
    	
    	valueString = object.getString(String.valueOf((int)UartScreenVar.addr_txt_parameters[6]));
    	liftRetractSpeed = Integer.parseInt(valueString);
   
    	valueString = object.getString(String.valueOf((int)UartScreenVar.addr_txt_parameters[7]));
    	delayTimeBeforeSolidify = Integer.parseInt(valueString);
    	
    	valueString = object.getString(String.valueOf((int)UartScreenVar.addr_txt_parameters[8]));
    	delayTimeAfterSolidify = Integer.parseInt(valueString);
    	
    	valueString = object.getString(String.valueOf((int)UartScreenVar.addr_txt_parameters[9]));
    	delayTimeForAirPump = Integer.parseInt(valueString);
    	
    	valueString = object.getString(String.valueOf((int)UartScreenVar.addr_txt_parameters[10]));
    	parameterEnabled = valueString.equals("true")?true:false;
    	
    	valueString = object.getString(String.valueOf((int)UartScreenVar.addr_txt_parameters[11]));
    	detectionEnabled = valueString.equals("true")?true:false;
    	
    	saveParameters();
    	
    }
    
    public boolean action_admin_login(String pwd)
    {
    	pwd = pwd.replaceAll("[^\\x20-\\x7E]", "");
    	if( pwd.equals("123") )
    		return true;
    	return false;
    }
    
}
