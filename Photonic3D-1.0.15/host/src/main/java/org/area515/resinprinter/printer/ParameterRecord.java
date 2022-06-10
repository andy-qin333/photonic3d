package org.area515.resinprinter.printer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;


/**
 * Created by zyd on 2017/10/10.
 */

@XmlRootElement(name="ParameterRecord")
public class ParameterRecord
{
	
    @XmlElement(name="LedUsedTime")
    private long ledUsedTime = 0;
    @XmlElement(name="ScreenUsedTime")
    private long screenUsedTime = 0;
    // FIXME: 2018/9/4 zyd add for multi language -s
    @XmlElement(name="Language")
    private int language = 0;
    // FIXME: 2018/9/4 zyd add for multi language -e
    @XmlElement(name="sumOfMaterial")
    private double sumOfMaterial = 0;
    //2019-11-11 derby add for sum of resine
    @XmlElement(name="layersCountClear")
    private int layersCountClear = 0;
  
    //2022/6/9 derby add for replace film
    @XmlElement(name="layersCountReplaceFilm")
    private int layersCountReplaceFilm = 0;
  
    // 2022/6/9 derby add for summary of replace film
    @XmlElement(name="cntReplaceFilm")
    private int cntReplaceFilm = 0;
 
    
    @XmlTransient
    public long getLedUsedTime()
    {
        return ledUsedTime;
    }
    public void setLedUsedTime(long ledUsedTime)
    {
        this.ledUsedTime = ledUsedTime;
    }

    @XmlTransient
    public long getScreenUsedTime()
    {
        return screenUsedTime;
    }
    public void setScreenUsedTime(long screenUsedTime)
    {
        this.screenUsedTime = screenUsedTime;
    }

    // FIXME: 2018/9/4 zyd add for multi language -s
    @XmlTransient
    public int getLanguage() {
        return language;
    }
    public void setLanguage(int language) {
        this.language = language;
    }
    // FIXME: 2018/9/4 zyd add for multi language -e
    
    @XmlTransient
    public double getSumOfMaterial()
    {
        return sumOfMaterial;
    }
    public void setSumOfMaterial(double sumOfMaterial)
    {
        this.sumOfMaterial = sumOfMaterial;
    }
    //2019-11-11 derby add for sum of resine
    @XmlTransient
    public int getlayersCountClear()
    {
        return layersCountClear;
    }
    public void setlayersCountClear(int  cnt)
    {
        this.layersCountClear = cnt;
    }
    //2019-11-11 derby add for clear action
    @XmlTransient
    public int getlayersReplaceFilm()
    {
        return layersCountReplaceFilm;
    }
    public void setlayersReplaceFilm(int  cnt)
    {
        this.layersCountReplaceFilm = cnt;
    }
    //2019-11-11 derby add for replace film
    @XmlTransient
    public int getCntReplaceFilm()
    {
        return cntReplaceFilm;
    }
    public void setCntReplaceFilm(int  cnt)
    {
        this.cntReplaceFilm = cnt;
    }
    //2019-11-11 derby add for summary of replace film
}
