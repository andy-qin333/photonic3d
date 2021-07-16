package org.area515.resinprinter.twodim;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.render.CurrentImageRenderer;

public class SimpleImageRenderer extends CurrentImageRenderer {
	public SimpleImageRenderer(DataAid aid, AbstractPrintFileProcessor<?, ?> processor, Object imageIndexToBuild) {
		super(aid, processor, imageIndexToBuild);
	}

	@Override
	public BufferedImage renderImage(BufferedImage image) throws JobManagerException {
		try {
//			return ImageIO.read((File)imageIndexToBuild);
			////modified by derby for mono screen using png files
			BufferedImage imgSource = ImageIO.read((File)imageIndexToBuild);
			BufferedImage monoImage = new BufferedImage(aid.xResolution/3, aid.yResolution, BufferedImage.TYPE_3BYTE_BGR);
            for(int i = 0; i < monoImage.getHeight(); i++) {
            	for(int j = 0; j < monoImage.getWidth(); j++) {
            		int pix0 = imgSource.getRGB(j*3+2, i);
            		int pix1 = imgSource.getRGB(j*3+1,i );
            		int pix2 = imgSource.getRGB(j*3,i );
            		pix0 = pix0&0x000000FF;
            		pix1 = pix1&0x0000FF00;
            		pix2 = pix2&0x00FF0000;
            		int monoPix = pix0 + pix1 + pix2;                		
            		monoPix = monoPix + 0xFF000000;
            		monoImage.setRGB(j, i, monoPix);
            	}
            }
            return monoImage;
		} catch (IOException e) {
			throw new JobManagerException("Unable to read image:" + imageIndexToBuild, e);
		}
	}
}
