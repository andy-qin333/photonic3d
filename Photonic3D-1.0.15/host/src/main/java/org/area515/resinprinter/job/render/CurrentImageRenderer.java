package org.area515.resinprinter.job.render;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;

import com.coremedia.iso.boxes.StaticChunkOffsetBox;

import org.area515.resinprinter.job.JobManagerException;

public abstract class CurrentImageRenderer implements Callable<RenderedData> {
	private static final Logger logger = LogManager.getLogger();
	protected Object imageIndexToBuild;
	protected AbstractPrintFileProcessor<?,?> processor;
	protected DataAid aid;
	
	public CurrentImageRenderer(DataAid aid, AbstractPrintFileProcessor<?,?> processor, Object imageIndexToBuild) {
		this.aid = aid;
		this.processor = processor;
		this.imageIndexToBuild = imageIndexToBuild;
	}
	
	public BufferedImage buildImage(int renderedWidth, int renderedHeight) {
		return new BufferedImage(renderedWidth, renderedHeight, BufferedImage.TYPE_4BYTE_ABGR);
	}
	
	public RenderedData call() throws JobManagerException {
		long startTime = System.currentTimeMillis();
		Lock lock = aid.cache.getSpecificLock(imageIndexToBuild);
		lock.lock();
		try {
			logger.info("rendering{}",imageIndexToBuild);
			RenderedData imageData = aid.cache.getOrCreateIfMissing(imageIndexToBuild);
			BufferedImage image = renderImage(imageData.getPreTransformedImage());
			//logger.info("rendering1");
			imageData.setPreTransformedImage(image);
			BufferedImage after = processor.applyImageTransforms(aid, image);
			imageData.setPrintableImage(after);
			//////add by derby9-27 for ds300
			splitImage(after, imageData);
			///////add by derby
			if (!aid.optimizeWithPreviewMode) {
				//long pixelArea = computePixelArea(image);
				//imageData.setArea((double)pixelArea);
				logger.info("Loaded {} with {} non-black pixels in {}ms", imageIndexToBuild, 1, System.currentTimeMillis()-startTime);
			}
			return imageData;
		} catch (ScriptException e) {
			logger.error(e);
			throw new JobManagerException("Unable to render image", e);
		} finally {
			lock.unlock();
			//logger.info("rendering2");
		}
	}
	
	abstract public BufferedImage renderImage(BufferedImage image) throws JobManagerException;

	/**
	 * Compute the number of non-black pixels in an image as a measure of its
	 * area as a pixel count. We can only handle 3 and 4 byte formats though.
	 * 
	 * @param image
	 * @return
	 */
	private long computePixelArea(BufferedImage image) throws JobManagerException {
		int type = image.getType();
		if (type == BufferedImage.TYPE_BYTE_BINARY)
			return 0;
		if (type != BufferedImage.TYPE_3BYTE_BGR
				&& type != BufferedImage.TYPE_4BYTE_ABGR
				&& type != BufferedImage.TYPE_4BYTE_ABGR_PRE
				&& type != BufferedImage.TYPE_BYTE_GRAY) {
			// BufferedImage is not any of the types that are currently supported.
			throw(new JobManagerException(
					"Slice image is not in a 3 or 4 byte BGR/ABGR format."
					+"Please open an issue about this and let us you know have an image of type: "
					+type)
					);
		}
		
		long area = 0;
		
		// We only need a count pixels, without regard to the X,Y orientation,
		// so use the method described at:
		// http://stackoverflow.com/questions/6524196/java-get-pixel-array-from-image
		// to get the byte buffer backing the BufferedImage and iterate through it
		boolean hasAlpha = image.getAlphaRaster() != null;
		byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		
		// Pixels are in groups of 3 if there is no alpha, 4 if there is an alpha
		int pixLen = 3;
		if (hasAlpha) {
			pixLen = 4;
		}
		
		// except for TYPE_BYTE_GRAY, where the pixel is just one byte
		if (type == BufferedImage.TYPE_BYTE_GRAY) {
			pixLen = 1;
		}
		
		// Iterate linearly across the pixels, summing up cases where the color
		// is not black (e.g. any color channel nonzero)
		for (int i = 0; i<pixels.length; i+=pixLen) {
			if (pixLen == 3) {
				if (pixels[i] != 0 || pixels[i+1] != 0 || pixels[i+2] != 0) {
					area++;
				}
			} else if (pixLen == 4) {
				if (pixels[i+1] != 0 || pixels[i+2] != 0 || pixels[i+3] != 0) {
					area++;
				}
			} else if (pixLen == 1) {
				if (pixels[i] != 0) {
					area++;
				}
			}
		}
		
		return area;
	}
	///////////////add by derby9-27 for ds300
	//////////////split the image/////////////////
	private void splitImage(BufferedImage image, RenderedData imageData) {
//		BufferedImage destImage1 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR); 
//		int[] maskArray = new int[image.getWidth()*image.getHeight()/2];
//		int[] destArray = new int[image.getWidth()*image.getHeight()/2];
//		destArray = image.getRGB(image.getWidth()/2, 0, image.getWidth()/2, image.getHeight(), destArray, 0, image.getWidth()/2);
//		destImage1.setRGB(image.getWidth()/2, 0, image.getWidth()/2, image.getHeight(), destArray, 0, image.getWidth()/2);
//		imageData.setPrintableImage(destImage1);
//		BufferedImage destImage2 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR); 
//		destArray = image.getRGB(0, 0, image.getWidth()/2, image.getHeight(), destArray, 0, image.getWidth()/2);
//		destImage2.setRGB(image.getWidth()/2, 0, image.getWidth()/2, image.getHeight(), maskArray, 0, image.getWidth()/2);
//		destImage2.setRGB(0, 0, image.getWidth()/2, image.getHeight(), destArray, 0, image.getWidth()/2);
//		imageData.setRemainImage(destImage2);
		
		///////speed the pixel operate//////
		////单灯板移动
		/*
		BufferedImage destImage1 = deepCopy(image);
		int[] maskArray = new int[image.getWidth()*image.getHeight()/2];
		destImage1.setRGB(image.getWidth()/2+1, 0, image.getWidth()/2-1, image.getHeight(), maskArray, 0, image.getWidth()/2-1);
		imageData.setSplitImage1(destImage1);
		BufferedImage destImage2 = deepCopy(image);
		destImage2.setRGB(0, 0, image.getWidth()/2, image.getHeight(), maskArray, 0, image.getWidth()/2);
		imageData.setSplitImage2(destImage2);
//		image.setRGB(0, 0, image.getWidth()/2, image.getHeight(), maskArray, 0, image.getWidth()/2);
//		imageData.setSplitImage2(image);
 */
		
		////双灯板方案 图像切割为两条
		int intScreen1 = 1293;
		int intScreen2 = 653;
		int intMask1 = 653;
		int intMask2 = 1241;
		int[] screenArray1 = new int[intScreen1*image.getHeight()];
		int[] screenArray2 = new int[intScreen2*image.getHeight()];
		int[] maskArray1 = new int[intMask1*image.getHeight()];
		int[] maskArray2 = new int[intMask2*image.getHeight()];
		
		BufferedImage destImage1 = deepCopy(image);
		destImage1.setRGB(0, 0, intScreen1, image.getHeight(), screenArray1, 0, intScreen1);
		destImage1.setRGB(intScreen1+intMask1, 0, intMask1, image.getHeight(), screenArray2, 0, intMask1);
		imageData.setSplitImage1(destImage1);
		
		BufferedImage destImage2 = deepCopy(image);
		destImage2.setRGB(intScreen1, 0, intMask1, image.getHeight(), maskArray1, 0, intMask1);
		destImage2.setRGB(intScreen1+intMask1+intScreen2, 0, intMask2, image.getHeight(), maskArray2, 0, intMask2);
		imageData.setSplitImage2(destImage2);
	}
	
	private BufferedImage deepCopy(BufferedImage srcImage) {
		BufferedImage b = new BufferedImage(srcImage.getWidth(), srcImage.getHeight(), srcImage.getType());
	    Graphics g = b.getGraphics();
	    g.drawImage(srcImage, 0, 0, null);
	    g.dispose();
	    return b;
	}
}


