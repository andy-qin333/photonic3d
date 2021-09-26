package org.area515.resinprinter.twodim;


import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;
import org.apache.commons.io.FileUtils;
import org.area515.resinprinter.job.AbstractPrintFileProcessor;
import org.area515.resinprinter.job.AbstractPrintFileProcessor.DataAid;
import org.area515.resinprinter.job.JobManagerException;
import org.area515.resinprinter.job.render.CurrentImageRenderer;
import org.area515.resinprinter.printer.SlicingProfile;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Created by zyd on 2017/10/12.
 */

public class SVGImageRender extends CurrentImageRenderer
{
    public SVGImageRender(DataAid aid, AbstractPrintFileProcessor<?, ?> processor, Object imageIndexToBuild) {
        super(aid, processor, imageIndexToBuild);
    }

    @Override
    public BufferedImage renderImage(BufferedImage image) throws JobManagerException
    {
    	return convertSVGToPNG();
    }

    public BufferedImage convertSVGToPNG() throws JobManagerException
    {
        try
        {
            final BufferedImage[] imagePointer = new BufferedImage[1];
      
            TranscodingHints transcoderHints = new TranscodingHints();
            transcoderHints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
            transcoderHints.put(ImageTranscoder.KEY_DOM_IMPLEMENTATION, SVGDOMImplementation.getDOMImplementation());
            transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI);
            transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg");
            transcoderHints.put(ImageTranscoder.KEY_WIDTH,  new Float(aid.xResolution));  //modify by derby,support different slice  resolutions
            transcoderHints.put(ImageTranscoder.KEY_HEIGHT, new Float(aid.yResolution));

            try
            {
                TranscoderInput input = new TranscoderInput(new FileInputStream((File)imageIndexToBuild));
                ImageTranscoder trans = new ImageTranscoder()
                {
                    @Override
                    public BufferedImage createImage(int w, int h)
                    {
                        return new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
                    }

                    @Override
                    public void writeImage(BufferedImage image, TranscoderOutput out) throws TranscoderException
                    {
                        BufferedImage image1 = new BufferedImage(image.getWidth()/3, image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
//                        Graphics2D graphics2D = image1.createGraphics();
//                        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//                        graphics2D.drawImage(image, null, 0, 0);
                        
                      //created by derby 2021-8-14, improve the code, 1 pixel(RGB) control->3 pixel
                        for(int i = 0; i < image1.getHeight(); i++) {
                        	for(int j = 0; j < image1.getWidth(); j++) {
                        		int pix0 = image.getRGB(j*3+2, i);
                        		int pix1 = image.getRGB(j*3+1,i );
                        		int pix2 = image.getRGB(j*3,i );
                        		if(pix0 == 0xFF000000 && pix1 == 0xFF000000 && pix2 == 0xFF000000)
                        			continue;
                        		if(pix0 != 0xFF000000)
                        			pix0 = pix0&0x000000FF;
                        		if(pix1 != 0xFF000000)
                        			pix1 = pix1&0x0000FF00;
                        		if(pix2 != 0xFF000000)
                        			pix2 = pix2&0x00FF0000;
                        		int monoPix = pix0 + pix1 + pix2; 
                        		image1.setRGB(j, i, monoPix);
                        	}
                        }
                        imagePointer[0] = image1;
//                      File outputfile = new File("derby-test-mono.png"); //derby8-12 png 图片在photoshop中可以像素对应，便于调试
//                      try {
//						ImageIO.write(imagePointer[0], "png", outputfile);
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
                    }
                };
                trans.setTranscodingHints(transcoderHints);
                trans.transcode(input, null);
                
//                File outputfile = new File("derby-test.png");
//                ImageIO.write(imagePointer[0], "png", outputfile);
              //created by derby 8-12, support the monoLCD, 1 pixel(RGB) control->3 pixel
//                BufferedImage monoImage = new BufferedImage(aid.xResolution/3, aid.yResolution, BufferedImage.TYPE_3BYTE_BGR);
//                for(int i = 0; i < monoImage.getHeight(); i++) {
//                	for(int j = 0; j < monoImage.getWidth(); j++) {
//                		int pix0 = imagePointer[0].getRGB(j*3+2, i);
//                		int pix1 = imagePointer[0].getRGB(j*3+1,i );
//                		int pix2 = imagePointer[0].getRGB(j*3,i );
//                		pix0 = pix0&0x000000FF;
//                		pix1 = pix1&0x0000FF00;
//                		pix2 = pix2&0x00FF0000;
//                		int monoPix = pix0 + pix1 + pix2;                		
//                		monoPix = monoPix + 0xFF000000;
//                		monoImage.setRGB(j, i, monoPix);
//                	}
//                }
//                imagePointer[0] = monoImage;
//                outputfile = new File("derby-test-mono.png"); //derby8-12 png 图片在photoshop中可以像素对应，便于调试
//                ImageIO.write(imagePointer[0], "png", outputfile);
            }
            catch (TranscoderException ex)
            {
                throw new IOException(imageIndexToBuild + " doesn't seem to be an SVG file.");
            }
            
            return imagePointer[0];
        }
        catch (IOException e)
        {
            throw new JobManagerException("Couldn't load image file:" + imageIndexToBuild, e);
        }
    }

}
