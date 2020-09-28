package org.area515.resinprinter.job.render;

import java.awt.image.BufferedImage;
import java.util.concurrent.locks.ReentrantLock;

public class RenderedData {
	private BufferedImage image;
	//add by derby9-27 for ds300 moving uvled
	private BufferedImage splitImage1; 
	private BufferedImage splitImage2; 
	////add by derby9-27
	private BufferedImage preTransformedImage;
	private Double area;
	private ReentrantLock lock = new ReentrantLock();

	public RenderedData() {
	}
	
	public void setPrintableImage(BufferedImage image) {
		this.image = image;
	}
	
	public BufferedImage getPrintableImage() {
		return this.image;
	}
	
	/////////////////add by derby9-27 for ds300
	public void setSplitImage1(BufferedImage image) {
		this.splitImage1 = image;
	}
	
	public void setSplitImage2(BufferedImage image) {
		this.splitImage2 = image;
	}
	
	public BufferedImage getSplitImage1() {
		return this.splitImage1;
	}
	
	public BufferedImage getSplitImage2() {
		return this.splitImage2;
	}
	
/////////////////add by derby9-27 for ds300
	
	
	public BufferedImage getPreTransformedImage() {
		return preTransformedImage;
	}
	public void setPreTransformedImage(BufferedImage preTransformedImage) {
		this.preTransformedImage = preTransformedImage;
	}

	public void setArea(Double area) {
		this.area = area;
	}
	public Double getArea() {
		return this.area;
	}
	
	public ReentrantLock getLock() {
		return lock;
	}
}
