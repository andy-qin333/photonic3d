package org.area515.resinprinter.printer;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.annotation.XmlTransient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.display.GraphicsOutputInterface;
import org.area515.resinprinter.display.InappropriateDeviceException;
import org.area515.resinprinter.printer.Printer.DisplayState;
import org.area515.resinprinter.gcode.GCodeControl;
import org.area515.resinprinter.job.JobStatus;
import org.area515.resinprinter.projector.ProjectorModel;
import org.area515.resinprinter.serial.SerialCommunicationsPort;
import org.area515.resinprinter.server.HostProperties;
import org.area515.resinprinter.uartscreen.UartScreenControl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Printer {
    private static final Logger logger = LogManager.getLogger();
	private PrinterConfiguration configuration;
	
	//For Display
	private GraphicsOutputInterface refreshFrame;
	private boolean started;
	private boolean shutterOpen;
	private int shutterTime;
	private Integer bulbHours;
	private long currentSlicePauseTime;
	private String displayDeviceID;
	
	//For Serial Ports
	private SerialCommunicationsPort printerFirmwareSerialPort;
	private SerialCommunicationsPort projectorSerialPort;
	// FIXME: 2017/9/1 zyd add for uartscreen -s
	private SerialCommunicationsPort uartScreenSerialPort;
	// FIXME: 2017/9/1 zyd add for uartscreen -e
	
	//For Job Status
	private volatile JobStatus status = JobStatus.Connecting;
	private ReentrantLock statusLock = new ReentrantLock();
	private Condition jobContinued = statusLock.newCondition();
	
	//GCode
	private GCodeControl gCodeControl;
	// FIXME: 2017/9/1 zyd add for uartscreen -s
	private UartScreenControl uartScreenControl;
	// FIXME: 2017/9/1 zyd add for uartscreen -e

	// FIXME: 2017/10/11 zyd add for record parameter -s
	private long startExposureTime = 0;
	// FIXME: 2017/10/11 zyd add for record parameter -e
	
	//2019-11-12 derby add for record the estimate resine volume
	private double volume = 0;

	//Projector model
	private ProjectorModel projectorModel;
	
	public static enum DisplayState {
		Calibration,
		Grid,
		Blank,
		CurrentSlice,
		Finished  ///derby add for synchronized the UVLed time  2019/11/13
	}
	
	//For jaxb/json
	@SuppressWarnings("unused")
	private Printer() {}
	
	public Printer(PrinterConfiguration configuration) throws InappropriateDeviceException {
		this.configuration = configuration;
		
		try {
			@SuppressWarnings("unchecked")
			Class<GCodeControl> gCodeClass = (Class<GCodeControl>)Class.forName("org.area515.resinprinter.gcode." + configuration.getMachineConfig().getMotorsDriverConfig().getDriverType() + "GCodeControl");
			gCodeControl = (GCodeControl)gCodeClass.getConstructors()[0].newInstance(this);
		} catch (ClassNotFoundException e) {
			throw new InappropriateDeviceException("Couldn't find GCode controller for:" + configuration.getMachineConfig().getMotorsDriverConfig().getDriverType(), e);
		} catch (SecurityException e) {
			throw new InappropriateDeviceException("No permission to create class for:" + configuration.getMachineConfig().getMotorsDriverConfig().getDriverType(), e);
		} catch (Exception e) {
			throw new InappropriateDeviceException("Couldn't create instance for:" + configuration.getMachineConfig().getMotorsDriverConfig().getDriverType(), e);
		}

		// FIXME: 2017/9/1 zyd add for uartscreen -s
		this.uartScreenControl = new UartScreenControl(this);
		// FIXME: 2017/9/1 zyd add for uartscreen -e
	}
	
	@JsonIgnore
	public String getName() {
		return configuration.getName();
	}
	
	@XmlTransient
	@JsonProperty
	public boolean isPrintInProgress() {
		return status != null && status.isPrintInProgress();
	}
	@JsonIgnore
	public void setPrintInProgress(boolean printInProgress) {
	}
	
	@XmlTransient
	@JsonProperty
	public boolean isPrintPaused() {
		return status != null && getStatus().isPaused();
	}
	@JsonIgnore
	public void setPrintPaused(boolean printInProgress) {
	}
	
	@XmlTransient
	@JsonIgnore
	public boolean isPrintActive() {
		return status != null && status.isPrintActive();
	}

	@XmlTransient
	@JsonProperty
	public boolean isStarted() {
		return started;
	}
	public void setStarted(boolean started) {
		this.started = started;
	}
	
	@XmlTransient
	@JsonProperty
	public JobStatus getStatus() {
		return status;
	}
	
	@JsonIgnore
	public void setStatus(JobStatus status) {
		statusLock.lock();
		try {
			if (this.status != null && this.status.isError())
				return;
			if (this.status != null && this.status.isPaused()) {
				jobContinued.signalAll();
			}
			logger.info("Moving from status:" + this.status + " to status:" + status);
			this.status = status;
			if (!status.isPrintInProgress() && refreshFrame != null) {
				refreshFrame.resetSliceCount();
			}
		} finally {
			statusLock.unlock();
		}
	}
	
	public boolean waitForPauseIfRequired() {
		statusLock.lock();
		try {
			//Very important that this check is performed
			if (this.status != null && !this.status.isPaused()) {
				return isPrintActive();
			}
			logger.info("Print has been paused.");
			long startPause = System.currentTimeMillis();
			jobContinued.await();
			currentSlicePauseTime += System.currentTimeMillis() - startPause;
			logger.info("Print has resumed.");
			return isPrintActive();
		} catch (InterruptedException e) {
			logger.error("Normal if os is shutting us down", e);
			return isPrintActive();
		} finally {
			statusLock.unlock();
		}
	}
	
	public JobStatus togglePause() {
		statusLock.lock();
		try {
			if (this.status != null && this.status.isPaused()) {
				setStatus(JobStatus.Printing);
				return this.status;
			}
			
			if (this.status == JobStatus.Printing) {
				setStatus(JobStatus.Paused);
			}

			return this.status;
		} finally {
			statusLock.unlock();
		}
	}
	
	public void initializeAndAssignGraphicsOutputInterface(final GraphicsOutputInterface device, final String displayDeviceID) {
		this.displayDeviceID = displayDeviceID;
		this.refreshFrame = device.initializeDisplay(displayDeviceID);// derby init display;maybe can add another display
		
		Rectangle screenSize = refreshFrame.getBoundary();
		getConfiguration().getMachineConfig().getMonitorDriverConfig().setDLP_X_Res(screenSize.width);
		getConfiguration().getMachineConfig().getMonitorDriverConfig().setDLP_Y_Res(screenSize.height);
	}
	
	public String getDisplayDeviceID() {
		return displayDeviceID;
	}
	
	public void showBlankImage() {	
		refreshFrame.showBlankImage();
	}
	
	public void showCalibrationImage(int xPixels, int yPixels) {
		refreshFrame.showCalibrationImage(xPixels, yPixels);
	}
	
	public void showGridImage(int pixels) {
		refreshFrame.showGridImage(pixels);
	}
	
	public void showImage(BufferedImage image) {
		refreshFrame.showImage(image);
	}
	
	public DisplayState getDisplayState() {
		return refreshFrame.getDisplayState();
	}
	
	@JsonIgnore
	@XmlTransient
	public boolean isDisplayBusy() {
		if (refreshFrame == null) {
			return false;
		}
		
		return refreshFrame.isDisplayBusy();
	}

	@JsonIgnore
	@XmlTransient
	public boolean isProjectorPowerControlSupported() {
		return projectorModel != null;
	}
	
	@JsonIgnore
	@XmlTransient
	public void setProjectorModel(ProjectorModel projectorModel) {
		this.projectorModel = projectorModel;
	}
	@JsonIgnore
	@XmlTransient
	
	public ProjectorModel getProjectorModel() {
		return projectorModel;
	}
	
	public void setProjectorPowerStatus(boolean powerOn) throws IOException {
		if (projectorModel == null) {
			throw new IOException("Projector model couldn't be detected");
		}
		
		if (projectorSerialPort == null) {
			throw new IOException("Serial port not available for projector.");
		}
		
		projectorModel.setPowerState(powerOn, projectorSerialPort);
	}

	public PrinterConfiguration getConfiguration() {
		return configuration;
	}
	public void setConfiguration(PrinterConfiguration configuration) {
		this.configuration = configuration;
	}

	public boolean isShutterOpen() {
		return shutterOpen;
	}
	public void setShutterOpen(boolean shutterOpen) {
		this.shutterOpen = shutterOpen;
	}
	public void setShutterTime(int shutterTime) {
		this.shutterTime = shutterTime;
	}
	public int getShutterTime() {
		return shutterTime;
	}
	
	
	@JsonIgnore
	public Integer getBulbHours() {
		if (bulbHours == null && projectorModel != null) {
			try {
				bulbHours = projectorModel.getBulbHours(projectorSerialPort);
			} catch (IOException e) {
				logger.error("Failed communicating with projector for bulb hours", e);
			}
		}
		
		return bulbHours;
	}
	public void setBulbHours(Integer bulbHours) {
		this.bulbHours = bulbHours;
	}
	
	public Integer getCachedBulbHours() {
		return bulbHours;
	}
	public void setCachedBulbHours(Integer bulbHours) {
		this.bulbHours = bulbHours;
	}
	
	public long getCurrentSlicePauseTime() {
		return currentSlicePauseTime;
	}
	public void setCurrentSlicePauseTime(long currentSlicePauseTime) {
		this.currentSlicePauseTime = currentSlicePauseTime;
	}
	
	@JsonIgnore
	public GCodeControl getGCodeControl() {
		return gCodeControl;
	}
	
	public boolean setPrinterFirmwareSerialPort(SerialCommunicationsPort printerFirmwareSerialPort) {
		boolean result = false;
		this.printerFirmwareSerialPort = printerFirmwareSerialPort;
		logger.info("Firmware serial port set to:" + printerFirmwareSerialPort);
		
		//Read the welcome mat if it's not null
		if (printerFirmwareSerialPort != null) {
			try {
				logger.info("Firmware Welcome chitchat:" + getGCodeControl().readWelcomeChitChat());
				result = true;
			} catch (IOException e) {
				logger.error("Error while reading welcome chitchat", e);
			}
		}
		return result;
	}
	@JsonIgnore
	public SerialCommunicationsPort getPrinterFirmwareSerialPort() {
		return printerFirmwareSerialPort;
	}
	
	public void setProjectorSerialPort(SerialCommunicationsPort projectorSerialPort) {
		this.projectorSerialPort = projectorSerialPort;
	}
	@JsonIgnore
	public SerialCommunicationsPort getProjectorSerialPort() {
		return projectorSerialPort;
	}

	// FIXME: 2017/9/1 zyd add for uartscreen -s
	public void setUartScreenSerialPort(SerialCommunicationsPort uartScreenSerialPort) {
		this.uartScreenSerialPort = uartScreenSerialPort;
		if (this.uartScreenSerialPort != null && this.uartScreenControl != null) {
			this.uartScreenControl.start();
		}
	}
	@JsonIgnore
	public SerialCommunicationsPort getUartScreenSerialPort() {
		return uartScreenSerialPort;
	}

	@JsonIgnore
	public UartScreenControl getUartScreenControl() {
		return uartScreenControl;
	}
	// FIXME: 2017/9/1 zyd add for uartscreen -e

	// FIXME: 2017/10/11 zyd add for record parameter -s
	public void startExposureTiming()
	{
		startExposureTime = System.currentTimeMillis();
	}

	public void stopExposureTiming()
	{
		long exposuredTime = System.currentTimeMillis() - startExposureTime;

		ParameterRecord parameterRecord;
		parameterRecord = HostProperties.Instance().getParameterRecord();
		parameterRecord.setLedUsedTime(parameterRecord.getLedUsedTime() + exposuredTime);
		parameterRecord.setScreenUsedTime(parameterRecord.getScreenUsedTime() + exposuredTime);
		parameterRecord.setlayersCountClear(parameterRecord.getlayersCountClear()+1);
		parameterRecord.setlayersReplaceFilm(parameterRecord.getlayersReplaceFilm()+1);
		parameterRecord.setSysRunningHours(parameterRecord.getSysRunningHours()+1);
		HostProperties.Instance().saveParameterRecord(parameterRecord);
	}

	@JsonIgnore
	public long getLedUsedTime()
	{
		ParameterRecord parameterRecord;
		parameterRecord = HostProperties.Instance().getParameterRecord();
		//parameterRecord.setLedUsedTime(10);  //wrong code.    derby.dai9-2
		return parameterRecord.getLedUsedTime();
	}

	public void setLedUsedTime(long usedTime)
	{
		ParameterRecord parameterRecord;
		parameterRecord = HostProperties.Instance().getParameterRecord();

		parameterRecord.setLedUsedTime(usedTime);
		HostProperties.Instance().saveParameterRecord(parameterRecord);
	}

	@JsonIgnore
	public long getScreenUsedTime()
	{
		ParameterRecord parameterRecord;
		parameterRecord = HostProperties.Instance().getParameterRecord();
		return parameterRecord.getScreenUsedTime();

	}

	public void setScreenUsedTime(long usedTime)
	{
		ParameterRecord parameterRecord;
		parameterRecord = HostProperties.Instance().getParameterRecord();

		parameterRecord.setScreenUsedTime(usedTime);
		HostProperties.Instance().saveParameterRecord(parameterRecord);

	}
	// FIXME: 2017/10/11 zyd add for record parameter -e
	
	@JsonIgnore
	public double getSumOfMaterial()
	{
		ParameterRecord parameterRecord;
		parameterRecord = HostProperties.Instance().getParameterRecord();
		return parameterRecord.getSumOfMaterial();

	}
	// FIXME: 2019-11-11 derby add for sum of material
	public void setSumOfMaterial(double volume)
	{
		ParameterRecord parameterRecord;
		parameterRecord = HostProperties.Instance().getParameterRecord();

		parameterRecord.setSumOfMaterial(volume);
		HostProperties.Instance().saveParameterRecord(parameterRecord);

	}
	
	public double getVolume()
	{
		return this.volume;
	}
	
	public void setVolume(double volume)
	{
		this.volume = volume;
	}
	
	public int getCntLayersClearResin()
	{
		return HostProperties.Instance().getParameterRecord().getlayersCountClear();
	}
	
	public void setCntLayersClearResin(int cnt)
	{
		ParameterRecord parameterRecord;
		parameterRecord = HostProperties.Instance().getParameterRecord();

		parameterRecord.setlayersCountClear(cnt);
		HostProperties.Instance().saveParameterRecord(parameterRecord);
	}
	
	public int getCntLayersReplacefilm()
	{
		return HostProperties.Instance().getParameterRecord().getlayersReplaceFilm();
	}
	
	public void setCntLayersReplacefilm(int cnt)
	{
		ParameterRecord parameterRecord;
		parameterRecord = HostProperties.Instance().getParameterRecord();

		parameterRecord.setlayersReplaceFilm(cnt);
		HostProperties.Instance().saveParameterRecord(parameterRecord);
	}
	
	public int getCntReplaceFilm()
	{
		return HostProperties.Instance().getParameterRecord().getCntReplaceFilm();
	}
	
	public void setCntReplaceFilm(int cnt)
	{
		ParameterRecord parameterRecord;
		parameterRecord = HostProperties.Instance().getParameterRecord();

		parameterRecord.setCntReplaceFilm(cnt);
		HostProperties.Instance().saveParameterRecord(parameterRecord);
	}
	
	public double getSysRunningHours()
	{
		return HostProperties.Instance().getParameterRecord().getSysRunningHours();
	}
	
	public void setSysRunningHours(Double cnt)
	{
		ParameterRecord parameterRecord;
		parameterRecord = HostProperties.Instance().getParameterRecord();

		parameterRecord.setSysRunningHours(cnt);
		HostProperties.Instance().saveParameterRecord(parameterRecord);
	}


	public String toString() {
		return getName() + "(printerFirmwareSerialPort:" + printerFirmwareSerialPort + ", projectorSerialPort:" + projectorSerialPort + " Display:" + displayDeviceID + ")";
	}
	
	public void close() {
		if (printerFirmwareSerialPort != null) {
			printerFirmwareSerialPort.close();
		}
		// FIXME: 2017/9/1 zyd add for uartscreen -s
		if (uartScreenControl != null) {
			uartScreenControl.close();
			uartScreenControl = null;
		}
		if (uartScreenSerialPort != null) {
			uartScreenSerialPort.close();
		}
		// FIXME: 2017/9/1 zyd add for uartscreen -e
		if (refreshFrame != null) {
			refreshFrame.dispose();
		}
		started = false;
	}

	public void disassociateDisplay() {
		this.bulbHours = null;
		this.refreshFrame = null;
		this.displayDeviceID = null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((configuration == null) ? 0 : configuration.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Printer other = (Printer) obj;
		if (configuration == null) {
			if (other.configuration != null)
				return false;
		} else if (!configuration.equals(other.configuration))
			return false;
		return true;
	}
}
