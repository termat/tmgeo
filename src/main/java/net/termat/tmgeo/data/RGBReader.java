package net.termat.tmgeo.data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.gdal.gdal.gdal;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

import net.termat.tmgeo.util.PCUtil;

public class RGBReader {
	private SpatialReference srs;
	private AffineTransform atrans;
	private BufferedImage band;
	private int epsg;
	private String chName;
	
	private RGBReader() throws IOException {}
	
	public static RGBReader createReader(int epsg,File f,String name) throws IOException {
		gdal.AllRegister();
		RGBReader sd=new RGBReader();
		sd.srs=BandUtil.createSpatialReference(epsg);
		sd.epsg=epsg;
		sd.band=ImageIO.read(f);
		sd.atrans=PCUtil.loadTransform(new File(f.getAbsolutePath().replace(".png", ".pgw").replace(".PNG", ".pgw").replace(".jpg", ".jgw").replace(".JPG", ".jgw")));
		sd.chName=name;
		return sd;
	}
	
	
	public static RGBReader createReader(int epsg,BufferedImage img,AffineTransform af,String name) throws IOException {
		gdal.AllRegister();
		RGBReader sd=new RGBReader();
		sd.srs=BandUtil.createSpatialReference(epsg);
		sd.epsg=epsg;
		sd.band=img;
		sd.atrans=af;
		sd.chName=name;
		return sd;
	}
	
	public RGBReader createSubImage(Rectangle2D rect,double res) throws IOException {
		double[] param=new double[] {res,0,0,-res,rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(param);
		RGBReader sd=new RGBReader();
		sd.srs=srs;
		sd.epsg=epsg;
		sd.atrans=af;
		sd.band=getBand(rect,res);
		sd.chName=chName;
		return sd;
	}
	
	public RGBReader createSubImage(Rectangle2D rect) throws IOException {
		double[] param=new double[] {atrans.getScaleX(),0,0,atrans.getScaleY(),rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(param);
		RGBReader sd=new RGBReader();
		sd.srs=srs;
		sd.epsg=epsg;
		sd.atrans=af;
		sd.band=getBand(rect);
		sd.chName=chName;
		return sd;
	}
	
	public BufferedImage getBand() {
		return band;
	}
	
	public BufferedImage getBand(Rectangle2D rect,double res) {
		int ww=band.getWidth();
		int hh=band.getHeight();
		int w=(int)Math.round(rect.getWidth()/res);
		int h=(int)Math.abs(Math.round(rect.getHeight()/res));
		BufferedImage ret=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		double[] param=new double[] {res,0,0,-res,rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(param);
		AffineTransform iaf=null;
		try {
			iaf=atrans.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		for(int x=0;x<w;x++) {
			for(int y=0;y<h;y++) {
				Point2D p=af.transform(new Point2D.Double(x, y), new Point2D.Double());
				p=iaf.transform(p, new Point2D.Double());
				int xx=(int)Math.round(p.getX());
				int yy=(int)Math.round(p.getY());
				if(xx>=0&&xx<ww&&yy>=0&&yy<hh) {
					ret.setRGB(x, y, band.getRGB(xx, yy));
				}
			}
		}
		return ret;
	}
	
	public BufferedImage getBand(Rectangle2D rect) {
		int w=(int)Math.round(rect.getWidth()/atrans.getScaleX());
		int h=(int)Math.abs(Math.round(rect.getHeight()/atrans.getScaleY()));
		BufferedImage ret=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		double[] param=new double[] {atrans.getScaleX(),0,0,atrans.getScaleY(),rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(param);
		AffineTransform iaf=null;
		try {
			iaf=atrans.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		int ww=band.getWidth();
		int hh=band.getHeight();
		for(int x=0;x<w;x++) {
			for(int y=0;y<h;y++) {
				Point2D p=af.transform(new Point2D.Double(x, y), new Point2D.Double());
				p=iaf.transform(p, new Point2D.Double());
				int xx=(int)Math.round(p.getX());
				int yy=(int)Math.round(p.getY());
				if(xx>=0&&xx<ww&&yy>=0&&yy<hh) {
					ret.setRGB(x, y, band.getRGB(xx, yy));
				}
			}
		}
		return ret;
	}
	
	public SpatialReference getSrs() {
		return srs;
	}
	
	public AffineTransform getTransform() {
		return atrans;
	}
	
	public BufferedImage getImage() {
		return band;
	}
	
	public int[][] getRGB(){
		int[][] ret=new int[band.getWidth()][band.getHeight()];
		for(int i=0;i<ret.length;i++) {
			for(int j=0;j<ret[0].length;j++) {
				ret[i][j]=band.getRGB(i, j);
			}
		}
		return ret;
	}
	
	
	public Rectangle2D getBounds() {
		Rectangle2D ret=new Rectangle2D.Double(0,0,band.getWidth(),band.getHeight());
		return atrans.createTransformedShape(ret).getBounds2D();
	}
	
	public int getEPSG() {
		return epsg;
	}
	
	public String getChannelName() {
		return chName;
	}
	
	public RGBReader createProjectionData(int target_epsg) throws IOException {
		SpatialReference target=BandUtil.createSpatialReference(target_epsg);
		CoordinateTransformation ct=BandUtil.getCoordinateTransformation(srs,target);
		Rectangle2D rect=null;
		for(int x=0;x<band.getWidth();x++) {
			for(int y=0;y<band.getHeight();y++) {
				Point2D sp=atrans.transform(new Point2D.Double(x,y), new Point2D.Double());
				double[] p2=ct.TransformPoint(sp.getX(), sp.getY());
				if(rect==null) {
					rect=new Rectangle2D.Double(p2[0],p2[1],0,0);
				}else {
					rect.add(p2[0],p2[1]);
				}
			}
		}
		double sx=rect.getWidth()/band.getWidth();
		double sy=rect.getHeight()/band.getHeight();
		double[] p=new double[] {sx,0,0,-sy,rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(p);
		int ww=(int)Math.abs(rect.getWidth()/sx);
		int hh=(int)Math.abs(rect.getHeight()/sy);
		AffineTransform iaf=null;
		try {
			iaf=atrans.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		RGBReader sd=new RGBReader();
		sd.srs=target;
		sd.atrans=af;
		sd.epsg=target_epsg;
		CoordinateTransformation ct2=BandUtil.getCoordinateTransformation(target,srs);
		sd.band=new BufferedImage(ww,hh,band.getType());
		int sw=sd.band.getWidth();
		int sh=sd.band.getHeight();
		for(int x=0;x<ww;x++) {
			for(int y=0;y<hh;y++) {
				Point2D p1=af.transform(new Point2D.Double(x,y), new Point2D.Double());
				double[] pt=ct2.TransformPoint(p1.getX(), p1.getY());
				Point2D p2=iaf.transform(new Point2D.Double(pt[0], pt[1]), new Point2D.Double());
				int xx=(int)p2.getX();
				int yy=(int)p2.getY();
				if(xx>=0&&xx<sw&&yy>=0&&yy<sh) {
					sd.band.setRGB(x, y, band.getRGB(xx, yy));
				}
			}
		}
		return sd;
	}
	
	public void setMaskImage(File mask) throws IOException {
		int col=Color.BLACK.getRGB();
		int val=new Color(0,0,0,0).getRGB();
		int w=band.getWidth();
		int h=band.getHeight();
		BufferedImage tmp=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
		
		BufferedImage img=ImageIO.read(mask);
		for(int i=0;i<w;i++) {
			for(int j=0;j<h;j++) {
				if(img.getRGB(i, j)==col) {
					tmp.setRGB(i, j, val);
				}else {
					tmp.setRGB(i, j, band.getRGB(i, j));
				}
			}
		}
		band=tmp;
	}
	
	public static RGBReader connectReader(RGBReader src,RGBReader dst) throws IOException {
		RGBReader sd=new RGBReader();
		sd.srs=src.srs;
		sd.epsg=src.epsg;
		AffineTransform af1=src.atrans;
		AffineTransform af2=dst.atrans;
		Rectangle2D rect=null;
		BufferedImage b1=src.getImage();
		BufferedImage b2=dst.getImage();
		if(rect==null)rect=getAppendBounds(b1,af1,b2,af2);
		sd.band=appendDataPoint(rect,b1,af1,b2,af2);
		double[] param=new double[] {af1.getScaleX(),0,0,af1.getScaleY(),rect.getX(),rect.getY()+rect.getHeight()};
		sd.atrans=new AffineTransform(param);
		sd.chName=src.chName;
		return sd;
	}
	
	public static RGBReader connectReader(RGBReader src,RGBReader dst,Color bg) throws IOException {
		RGBReader sd=new RGBReader();
		sd.srs=src.srs;
		sd.epsg=src.epsg;
		AffineTransform af1=src.atrans;
		AffineTransform af2=dst.atrans;
		Rectangle2D rect=null;
		BufferedImage b1=src.getImage();
		BufferedImage b2=dst.getImage();
		if(rect==null)rect=getAppendBounds(b1,af1,b2,af2);
		sd.band=appendDataPoint(rect,b1,af1,b2,af2,bg);
		double[] param=new double[] {af1.getScaleX(),0,0,af1.getScaleY(),rect.getX(),rect.getY()+rect.getHeight()};
		sd.atrans=new AffineTransform(param);
		sd.chName=src.chName;
		return sd;
	}

	private static Rectangle2D getAppendBounds(BufferedImage f1,AffineTransform af1,BufferedImage f2,AffineTransform af2) {
		Rectangle2D rect=null;
		for(int i=0;i<f1.getWidth();i++) {
			for(int j=0;j<f1.getHeight();j++) {
				Point2D p=af1.transform(new Point2D.Double(i, j), new Point2D.Double());
				if(rect==null) {
					rect=new Rectangle2D.Double(p.getX(),p.getY(),0,0);
				}else {
					rect.add(p);
				}
			}
		}
		for(int i=0;i<f2.getWidth();i++) {
			for(int j=0;j<f2.getHeight();j++) {
				Point2D p=af2.transform(new Point2D.Double(i, j), new Point2D.Double());
				rect.add(p);
			}
		}
		return rect;
	}
	
	
	private static BufferedImage appendDataPoint(Rectangle2D rect,BufferedImage f1,AffineTransform af1,BufferedImage f2,AffineTransform af2) {
		double[] param=new double[] {af1.getScaleX(),0,0,af1.getScaleY(),rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(param);
		int ww=(int)Math.abs(rect.getWidth()/af.getScaleX());
		int hh=(int)Math.abs(rect.getHeight()/af.getScaleY());
		BufferedImage ret=new BufferedImage(ww,hh,BufferedImage.TYPE_INT_RGB);
		AffineTransform iaf1=null,iaf2=null;
		try {
			iaf1=af1.createInverse();
			iaf2=af2.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		int w1=f1.getWidth();
		int h1=f1.getHeight();
		int w2=f2.getWidth();
		int h2=f2.getHeight();
		for(int i=0;i<ww;i++) {
			for(int j=0;j<hh;j++) {
				Point2D p=af.transform(new Point2D.Double(i, j), new Point2D.Double());
				Point2D pt=iaf1.transform(p, new Point2D.Double());
				int xx=(int)Math.floor(pt.getX());
				int yy=(int)Math.floor(pt.getY());
				if(xx>=0&&xx<w1&&yy>=0&&yy<h1) {
					ret.setRGB(i, j, f1.getRGB(xx, yy));
				}
				pt=iaf2.transform(p, new Point2D.Double());
				xx=(int)Math.floor(pt.getX());
				yy=(int)Math.floor(pt.getY());
				if(xx>=0&&xx<w2&&yy>=0&&yy<h2) {
					ret.setRGB(i, j, f2.getRGB(xx, yy));
				}
			}
		}
		return ret;
	}
	
	private static BufferedImage appendDataPoint(Rectangle2D rect,BufferedImage f1,AffineTransform af1,BufferedImage f2,AffineTransform af2,Color bg) {
		double[] param=new double[] {af1.getScaleX(),0,0,af1.getScaleY(),rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(param);
		int ww=(int)Math.abs(rect.getWidth()/af.getScaleX());
		int hh=(int)Math.abs(rect.getHeight()/af.getScaleY());
		BufferedImage ret=new BufferedImage(ww,hh,BufferedImage.TYPE_INT_RGB);
		Graphics2D g=ret.createGraphics();
		g.setBackground(bg);
		g.clearRect(0, 0, ww, hh);
		g.dispose();
		AffineTransform iaf1=null,iaf2=null;
		try {
			iaf1=af1.createInverse();
			iaf2=af2.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		int w1=f1.getWidth();
		int h1=f1.getHeight();
		int w2=f2.getWidth();
		int h2=f2.getHeight();
		for(int i=0;i<ww;i++) {
			for(int j=0;j<hh;j++) {
				Point2D p=af.transform(new Point2D.Double(i, j), new Point2D.Double());
				Point2D pt=iaf1.transform(p, new Point2D.Double());
				int xx=(int)Math.round(pt.getX());
				int yy=(int)Math.round(pt.getY());
				if(xx>=0&&xx<w1&&yy>=0&&yy<h1) {
					ret.setRGB(i, j, f1.getRGB(xx, yy));
				}
				pt=iaf2.transform(p, new Point2D.Double());
				xx=(int)Math.round(pt.getX());
				yy=(int)Math.round(pt.getY());
				if(xx>=0&&xx<w2&&yy>=0&&yy<h2) {
					ret.setRGB(i, j, f2.getRGB(xx, yy));
				}
			}
		}
		return ret;
	}
}
