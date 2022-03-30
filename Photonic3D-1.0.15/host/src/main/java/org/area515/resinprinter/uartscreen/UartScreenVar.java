package org.area515.resinprinter.uartscreen;

//import org.area515.resinprinter.printer.Language;


/**
 * modified by derby 2020/12/16 for new DWIN 10.1 panel, new address
 */

public class UartScreenVar
{
	static final char addr_btn_mainUI = 0x0007;
    static final char addr_btn_file_ctrl = 0x0008;
    static final char addr_btn_file_usb_ctrl = 0x0009;
    static final char addr_btn_info_success = 0x001C;
    static final char addr_btn_print_ctrl = 0x0017;   //
    static final char addr_btn_print_pause = 0x0012;
    static final char addr_btn_info_fail = 0x001D;
    static final char addr_btn_print_suretoStop = 0x001A;
    static final char addr_btn_network = 0x000C;
    static final char addr_btn_network_psk = 0x000D;
    static final char addr_btn_language = 0x000E;
    static final char addr_btn_move_control = 0x000F;
    static final char addr_btn_optical_control = 0x0010;
    static final char addr_btn_parameters = 0x0013;
    static final char addr_btn_replace_part = 0x0014;
    static final char addr_btn_led_pwm_adjust = 0x0015;
    static final char addr_btn_clear_trough = 0x0016;
    static final char addr_btn_about = 0x000B;
    static final char addr_btn_update_software = 0x0018;
    static final char addr_btn_update_firmware = 0x0019;
    static final char addr_btn_update_software_failed = 0x0023;
    static final char addr_btn_update_firmware_failed = 0x0024;

    static final char[] addr_icon_prog = {0x5100, 0x5101, 0x5102, 0x5103, 0x5104};
    static final char addr_icon_pause[] = {0x0007,0x0006};
    static final char addr_icon_stop[] = {0x0007,0x000A};
    static final char addr_icon_parameter_enabled[] = {0x0013,0x0011};
    static final char addr_icon_detection_enabled = 0x5121;
    static final char addr_icon_led_board[] = {0x0010,0x0007};
    static final char addr_icon_water_pump[] = {0x0010,0x0008};
    static final char addr_icon_image_logo[] = {0x0010,0x0009};
    static final char addr_icon_image_full[] = {0x0010,0x000A};
    static final char[] addr_icon_printProgress = {0x0007,0x0011}; //add by derby 2022/3/28
    static final char addr_icon_printProgress_ex = 0x5142; //add by derby 2020/9/24 for ds300
    static final char addr_icon_lifetime_led [] = {0x0014,0x0006}; //add by derby 2020/1/14
    static final char addr_icon_lifetime_screen[] = {0x0014,0x0007}; //add by derby 2020/1/14
    //add by derby 2020/2/18 {hour_H，hour_L,min_H,min_L,sec_H,sec_L}
    static final char[][] addr_icon_printTime = {{0x0007,0x000B},{0x0007,0x000C},{0x0007,0x000D},{0x0007,0x000E},{0x0007,0x000F},{0x0007,0x0010}};
    
    
    static final char[] addr_txt_machineStatus = {0x0007,0x0007};
    static final char[] addr_txt_printFileName = {0x0007,0x0008};
    static final char addr_txt_printTime = 0x6040;  
    static final char[] addr_txt_printProgress = {0x0007,0x0009};
    static final char[][] addr_txt_fileList = {{0x0008,0x000B},{0x0008,0x000C},{0x0008,0x000D},{0x0008,0x000E}};
    static final char[][] addr_txt_usb_fileList = {{0x0009,0x000B},{0x0009,0x000C},{0x0009,0x000D},{0x0009,0x000E}};
    static final char addr_txt_filePage = 0x61A0;
    static final char addr_txt_hardware_version[] = {0x000B,0x0007};
    static final char addr_txt_software_version[] = {0x000B,0x0008};
    static final char addr_txt_ipAddress[] = {0x000B,0x0009};
    static final char addr_txt_modelNumber[] = {0x000B,0x0006};
    static final char addr_txt_serialNumber[] = {0x000B,0x000A};  //add by derby 6-7 for serial number
    static final char[][] addr_txt_network_List = {{0x000C,0x0006},{0x000C,0x0007},{0x000C,0x0008},{0x000C,0x0009},{0x000C,0x000A}};
    static final char addr_txt_networkSsid[] = {0x000D,0x0008};
    //static final char addr_txt_networkPsk = 0x63C0;
    static final char addr_txt_networkPsk = 0x63bf; //modified by derby 2021/3/8 for dwin new screen
    static final char[] addr_txt_material = {0x6400, 0x6410, 0x6420, 0x6430, 0x6440, 0x6450, 0x6460};
    static final char addr_txt_led_temperature[] = {0x0010,0x0006};
    static final char addr_txt_admin_password = 0x0011;
    static final char[][] addr_txt_parameters = {{0x0013,0x0006},{0x0013,0x0007},{0x0013,0x0008},{0x0013,0x0009},{0x0013,0x000A},
    		{0x0013,0x000B},{0x0013,0x000C},{0x0013,0x000D},{0x0013,0x000E},{0x0013,0x000F}};
    static final char addr_txt_lifetime_led[] = {0x0014,0x0008};
    static final char addr_txt_lifetime_screen[] = {0x0014,0x0009};
    static final char addr_txt_led_pwm[] = {0x0015,0x0009};
    static final char addr_txt_pwm_slider[] = {0x0015,0x000A};

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
        update_software_failed,
        update_firmware_failed,
        Main,
        LocalFile,
        UdiskFile,
        Settings,
        About,
        Networks,
        NetworkEdit,
        Admin,
        Resin_clean,//used for resin clean's pop window auto-close
        operation_fail,
        operation_success,
        no_operation,
        replacement,
        led_pwm,
        connect_wifi_succes,
        connect_wifi_failed,
        connecting,
        sureToStop
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
            pos = 6;
        if (pagePos == PagePos.Updating)
            pos = 34;
        if (pagePos == PagePos.Updated)
            pos = 31;
        if (pagePos == PagePos.Main)
            pos = 7;
        if (pagePos == PagePos.LocalFile)
            pos = 8;
        if (pagePos == PagePos.UdiskFile)
            pos = 9;
        if (pagePos == PagePos.Settings)
            pos = 10;
        if (pagePos == PagePos.About)
            pos = 11;
        if (pagePos == PagePos.Networks)
            pos = 12;
        if (pagePos == PagePos.NetworkEdit)
            pos = 13;
        if (pagePos == PagePos.Admin)
            pos = 18;
        if (pagePos == PagePos.operation_fail)
        	pos = 29;
        if (pagePos == pagePos.operation_success)
        	pos = 28;
        if (pagePos == pagePos.no_operation)
        	pos = 27;
        if (pagePos == pagePos.Resin_clean)
        	pos = 32;
        if (pagePos == pagePos.replacement)
        	pos = 20;
		if (pagePos == pagePos.led_pwm)
		    pos = 21;
		if (pagePos == pagePos.update_software_failed)
		    pos = 35;
		if (pagePos == pagePos.update_firmware_failed)
		    pos = 36;
		if (pagePos == pagePos.connect_wifi_succes)
		    pos = 37;
		if (pagePos == pagePos.connect_wifi_failed)
		    pos = 38;
		if (pagePos == pagePos.connecting)
		    pos = 33;
		if (pagePos == pagePos.sureToStop)
		    pos = 26;
//        if (pagePos != PagePos.Loading && lang == 1){
//        	 if(ModelNumber.equals("3DTALK_DS200_MONO"))
//        		 pos += 32;
//        	 else
//        		 pos += 27;
//        }

        return pos;
    }

}
