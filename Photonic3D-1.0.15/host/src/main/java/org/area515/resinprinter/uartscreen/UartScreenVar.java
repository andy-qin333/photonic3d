package org.area515.resinprinter.uartscreen;

import org.area515.resinprinter.printer.Language;

/**
 * Created by zyd on 2018/9/4.
 */
/*
public class UartScreenVar
{
    static final char addr_btn_file_ctrl = 0x0002;
    static final char addr_btn_print_ctrl = 0x0003;
    static final char addr_btn_network = 0x0004;
    static final char addr_btn_language = 0x0005;
    static final char addr_btn_move_control = 0x0006;
    static final char addr_btn_optical_control = 0x0007;
    static final char addr_btn_parameters = 0x0008;
    static final char addr_btn_replace_part = 0x0009;
    static final char addr_btn_led_pwm_adjust = 0x000A;
    static final char addr_btn_clear_trough = 0x000B;
    static final char addr_btn_about = 0x000C;
    static final char addr_btn_update_software = 0x000D;
    static final char addr_btn_update_firmware = 0x000E;

    static final char[] addr_icon_prog = {0x0100, 0x0101, 0x0102, 0x0103, 0x0104};
    static final char addr_icon_pause = 0x0110;
    static final char addr_icon_stop = 0x0111;
    static final char addr_icon_parameter_enabled = 0x0120;
    static final char addr_icon_detection_enabled = 0x0121;
    static final char addr_icon_led_board = 0x0130;
    static final char addr_icon_water_pump = 0x0131;
    static final char addr_icon_image_logo = 0x0132;
    static final char addr_icon_image_full = 0x0133;
    static final char addr_icon_printProgress = 0x0143; //add by derby 2020/1/14
    static final char addr_icon_printProgress_ex = 0x0142; //add by derby 2021/2/26 for ds300 uartscreen
    static final char addr_icon_lifetime_led = 0x0140; //add by derby 2020/1/14
    static final char addr_icon_lifetime_screen = 0x0141; //add by derby 2020/1/14
    //add by derby 2020/2/18 {hour_H，hour_L,min_H,min_L,sec_H,sec_L}
    static final char[] addr_icon_printTime = {0x0144, 0x0145, 0x0146, 0x0147, 0x0148, 0x0149};
    
    
    static final char addr_txt_machineStatus = 0x1000;
    static final char addr_txt_printFileName = 0x1020;
    static final char addr_txt_printTime = 0x1040;  
    static final char addr_txt_printProgress = 0x1060;
    static final char[] addr_txt_fileList = {0x1100, 0x1120, 0x1140, 0x1160, 0x1180};
    static final char addr_txt_filePage = 0x11A0;
    static final char addr_txt_hardware_version = 0x1200;
    static final char addr_txt_software_version = 0x1210;
    static final char addr_txt_ipAddress = 0x1220;
    static final char addr_txt_modelNumber = 0x1230;
    static final char[] addr_txt_network_List = {0x1300, 0x1320, 0x1340, 0x1360, 0x1380};
    static final char addr_txt_networkSsid = 0x13A0;
    static final char addr_txt_networkPsk = 0x13C0;
    static final char[] addr_txt_material = {0x1400, 0x1410, 0x1420, 0x1430, 0x1440, 0x1450, 0x1460};
    static final char addr_txt_led_temperature = 0x1500;
    static final char addr_txt_admin_password = 0x1600;
    static final char[] addr_txt_parameters = {0x1700, 0x1710, 0x1720, 0x1730, 0x1740, 0x1750, 0x1760, 0x1770, 0x1780, 0x1790, 0x17A0};
    static final char addr_txt_lifetime_led = 0x1800;
    static final char addr_txt_lifetime_screen = 0x1810;
    static final char addr_txt_led_pwm = 0x1900;

    static final char[] desc_txt_fileList = {0x4003, 0x4023, 0x4043, 0x4063, 0x4083};
    static final char[] desc_txt_network_list = {0x4103, 0x4123, 0x4143, 0x4163, 0x4183};
    */



/**
 * modified by derby 2020/12/16 for new DWIN 10.1 panel, new address
 */

public class UartScreenVar
{
    static final char addr_btn_file_ctrl = 0x5002;
    static final char addr_btn_print_ctrl = 0x5003;
    static final char addr_btn_network = 0x5004;
    static final char addr_btn_language = 0x5005;
    static final char addr_btn_move_control = 0x5006;
    static final char addr_btn_optical_control = 0x5007;
    static final char addr_btn_parameters = 0x5008;
    static final char addr_btn_replace_part = 0x5009;
    static final char addr_btn_led_pwm_adjust = 0x500A;
    static final char addr_btn_clear_trough = 0x500B;
    static final char addr_btn_about = 0x500C;
    static final char addr_btn_update_software = 0x500D;
    static final char addr_btn_update_firmware = 0x500E;

    static final char[] addr_icon_prog = {0x5100, 0x5101, 0x5102, 0x5103, 0x5104};
    static final char addr_icon_pause = 0x5110;
    static final char addr_icon_stop = 0x5111;
    static final char addr_icon_parameter_enabled = 0x5120;
    static final char addr_icon_detection_enabled = 0x5121;
    static final char addr_icon_led_board = 0x5130;
    static final char addr_icon_water_pump = 0x5131;
    static final char addr_icon_image_logo = 0x5132;
    static final char addr_icon_image_full = 0x5133;
    static final char addr_icon_printProgress = 0x5143; //add by derby 2020/1/14
    static final char addr_icon_printProgress_ex = 0x5142; //add by derby 2020/9/24 for ds300
    static final char addr_icon_lifetime_led = 0x5140; //add by derby 2020/1/14
    static final char addr_icon_lifetime_screen = 0x5141; //add by derby 2020/1/14
    //add by derby 2020/2/18 {hour_H，hour_L,min_H,min_L,sec_H,sec_L}
    static final char[] addr_icon_printTime = {0x5144, 0x5145, 0x5146, 0x5147, 0x5148, 0x5149};
    
    
    static final char addr_txt_machineStatus = 0x6000;
    static final char addr_txt_printFileName = 0x6020;
    static final char addr_txt_printTime = 0x6040;  
    static final char addr_txt_printProgress = 0x6060;
    static final char[] addr_txt_fileList = {0x6100, 0x6120, 0x6140, 0x6160, 0x6180};
    static final char addr_txt_filePage = 0x61A0;
    static final char addr_txt_hardware_version = 0x6200;
    static final char addr_txt_software_version = 0x6210;
    static final char addr_txt_ipAddress = 0x6220;
    static final char addr_txt_modelNumber = 0x6230;
    static final char[] addr_txt_network_List = {0x6300, 0x6320, 0x6340, 0x6360, 0x6380};
    static final char addr_txt_networkSsid = 0x63A0;
    static final char addr_txt_networkPsk = 0x63C0;
    static final char[] addr_txt_material = {0x6400, 0x6410, 0x6420, 0x6430, 0x6440, 0x6450, 0x6460};
    static final char addr_txt_led_temperature = 0x6500;
    static final char addr_txt_admin_password = 0x6600;
    static final char[] addr_txt_parameters = {0x6700, 0x6710, 0x6720, 0x6730, 0x6740, 0x6750, 0x6760, 0x6770, 0x6780, 0x6790, 0x67A0};
    static final char addr_txt_lifetime_led = 0x6800;
    static final char addr_txt_lifetime_screen = 0x6810;
    static final char addr_txt_led_pwm = 0x6900;

    static final char[] desc_txt_fileList = {0x4003, 0x4023, 0x4043, 0x4063, 0x4083};
    static final char[] desc_txt_network_list = {0x4103, 0x4123, 0x4143, 0x4163, 0x4183};

    public enum IconPos {
        Empty0,
        Print,
        Pause,
        Stop,
        Empty1,
        LightSwitch,
        WaterSwitch,
        PresetImage,
        FullScreenImage
    }

    public enum PagePos{
        Loading,
        Updating,
        Updated,
        Main,
        LocalFile,
        UdiskFile,
        Settings,
        About,
        Networks,
        NetworkEdit,
        Admin,
        Resin_clean//used for resin clean's pop window auto-close
    }

    public static int getIconPos(int lang, String ModelNumber, IconPos iconPos) {//derby2020-6-22 modify for more Models
        int pos = 0;
        switch(lang) {
        case 0: //CN中文
        	if(ModelNumber.equals("3DTALK_DF200") || ModelNumber.equals("3DTALK_DS200_MONO")) {
        		if (iconPos == IconPos.Empty0)
	                pos = 68;
	            else if (iconPos == IconPos.Print)
	                pos = 69;
	            else if (iconPos == IconPos.Pause)
	                pos = 70;
	            else if (iconPos == IconPos.Stop)
	                pos = 71;
	            else if (iconPos == IconPos.Empty1)
	                pos = 66;
	            else if (iconPos == IconPos.LightSwitch)
	                pos = 67;
	            else if (iconPos == IconPos.WaterSwitch)
	                pos = 67;
	            else if (iconPos == IconPos.PresetImage)
	                pos = 67;
	            else if (iconPos == IconPos.FullScreenImage)
	                pos = 67;
        	}
        	else if(ModelNumber.equals("3DTALK_DS200"))  {
        		if (iconPos == IconPos.Empty0)
	                pos = 68;
	            else if (iconPos == IconPos.Print)
	                pos = 69;
	            else if (iconPos == IconPos.Pause)
	                pos = 70;
	            else if (iconPos == IconPos.Stop)
	                pos = 71;
	            else if (iconPos == IconPos.Empty1)
	                pos = 66;
	            else if (iconPos == IconPos.LightSwitch)
	                pos = 73;
	            else if (iconPos == IconPos.WaterSwitch)
	                pos = 74;
	            else if (iconPos == IconPos.PresetImage)
	                pos = 75;
	            else if (iconPos == IconPos.FullScreenImage)
	                pos = 76;
        	}
        	break;
        	
        case 1://EN英文
        	if(ModelNumber.equals("3DTALK_DF200") || ModelNumber.equals("3DTALK_DS200_MONO")) {
	        	if (iconPos == IconPos.Empty0)
	                pos = 68;
	            else if (iconPos == IconPos.Print)
	                pos = 69;
	            else if (iconPos == IconPos.Pause)
	                pos = 70;
	            else if (iconPos == IconPos.Stop)
	                pos = 71;
	            else if (iconPos == IconPos.Empty1)
	                pos = 66;
	            else if (iconPos == IconPos.LightSwitch)
	                pos = 67;
	            else if (iconPos == IconPos.WaterSwitch)
	                pos = 67;
	            else if (iconPos == IconPos.PresetImage)
	                pos = 67;
	            else if (iconPos == IconPos.FullScreenImage)
	                pos = 67;
        	}
        	else if(ModelNumber.equals("3DTALK_DS200")) {
        		if (iconPos == IconPos.Empty0)
	                pos = 68;
	            else if (iconPos == IconPos.Print)
	                pos = 77;
	            else if (iconPos == IconPos.Pause)
	                pos = 78;
	            else if (iconPos == IconPos.Stop)
	                pos = 79;
	            else if (iconPos == IconPos.Empty1)
	                pos = 66;
	            else if (iconPos == IconPos.LightSwitch)
	                pos = 80;
	            else if (iconPos == IconPos.WaterSwitch)
	                pos = 81;
	            else if (iconPos == IconPos.PresetImage)
	                pos = 82;
	            else if (iconPos == IconPos.FullScreenImage)
	                pos = 83;
        	}
        	break;
        case 2://RU俄文
        	break;
        }
        return pos;
    }

    public static int getPagePos(int lang, String ModelNumber, PagePos pagePos) {
        int pos = 0;

        if (pagePos == PagePos.Loading)
            pos = 0;
        if (pagePos == PagePos.Updating)
            pos = 6;
        if (pagePos == PagePos.Updated)
            pos = 7;
        if (pagePos == PagePos.Main)
            pos = 8;
        if (pagePos == PagePos.LocalFile)
            pos = 9;
        if (pagePos == PagePos.UdiskFile)
            pos = 10;
        if (pagePos == PagePos.Settings)
            pos = 11;
        if (pagePos == PagePos.About)
            pos = 12;
        if (pagePos == PagePos.Networks)
            pos = 13;
        if (pagePos == PagePos.NetworkEdit)
            pos = 14;
        if (pagePos == PagePos.Admin)
            pos = 19;

        if (pagePos != PagePos.Loading && lang == 1){
        	 if(ModelNumber.equals("3DTALK_DS200_MONO"))
        		 pos += 32;
        	 else
        		 pos += 27;
        }

        return pos;
    }

}
