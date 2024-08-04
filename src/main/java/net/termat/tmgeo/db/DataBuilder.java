package net.termat.tmgeo.db;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.termat.tmgeo.data.BandReader;
import net.termat.tmgeo.util.PCUtil;

public class DataBuilder {

	public static Pair create(BandReader br,int chennel,String name,String type) {
		Index i=new Index();
		i.epsg=br.getEPSG();
		i.setTransform(br.getTransform());
		float[][] val=br.getBand(chennel);
		i.width=val.length;
		i.height=val[0].length;
		i.name=name;
		i.type=type;
		i.channel=1;
		i.key=System.currentTimeMillis();
		DBData d=DBData.create(val);
		d.key=i.key;
		return new Pair(i,d);
	}
	
	public static Pair create(BandReader br,String name,String type) {
		Index ix=new Index();
		ix.epsg=br.getEPSG();
		ix.setTransform(br.getTransform());
		if(br.getBandNum()==1) {
			float[][] val=br.getBand(0);
			ix.width=val.length;
			ix.height=val[0].length;
			ix.name=name;
			ix.type=type;
			ix.channel=1;
			ix.key=System.currentTimeMillis();
			DBData d=DBData.create(val);
			d.key=ix.key;
			return new Pair(ix,d);
		}else {
			float[][][] val=new float[br.getBandNum()][][];
			for(int i=0;i<br.getBandNum();i++) {
				val[i]=br.getBand(i);
			}
			ix.width=val[0].length;
			ix.height=val[0][0].length;
			ix.name=name;
			ix.type=type;
			ix.channel=val.length;
			ix.key=System.currentTimeMillis();
			DBData d=DBData.create(val);
			d.key=ix.key;
			return new Pair(ix,d);
		}
	}
	
	public static Pair create(BufferedImage img,String ext,AffineTransform af,int epsg,String name,String type) throws IOException {
		Index ix=new Index();
		ix.epsg=epsg;
		ix.setTransform(af);
		ix.width=img.getWidth();
		ix.height=img.getHeight();
		ix.name=name;
		ix.type=type;
		ix.channel=1;
		ix.key=System.currentTimeMillis();
		DBData d=DBData.create(img,ext);
		d.key=ix.key;
		return new Pair(ix,d);
	}
	
	public static Pair create(File f,int epsg,String name,String type) throws IOException {
		String[] line=f.getName().split(".");
		String ext=line[line.length-1];
		Index ix=new Index();
		ix.epsg=epsg;
		File af=null;
		DBData data=null;
		if(ext.equals("jpg")) {
			BufferedImage bi=ImageIO.read(f);
			data=DBData.create(bi, ext);
			af=new File(f.getAbsolutePath().replace("."+ext, ".jgw"));
			ix.setTransform(PCUtil.loadTransform(af));
			ix.width=bi.getWidth();
			ix.height=bi.getHeight();
		}else if(ext.equals("jpeg")) {
			BufferedImage bi=ImageIO.read(f);
			data=DBData.create(bi, ext);
			af=new File(f.getAbsolutePath().replace("."+ext, ".jgw"));
			ix.setTransform(PCUtil.loadTransform(af));
			ix.width=bi.getWidth();
			ix.height=bi.getHeight();
		}else if(ext.equals("png")) {
			BufferedImage bi=ImageIO.read(f);
			data=DBData.create(bi, ext);
			af=new File(f.getAbsolutePath().replace("."+ext, ".pgw"));
			ix.setTransform(PCUtil.loadTransform(af));
			ix.width=bi.getWidth();
			ix.height=bi.getHeight();
		}else if(ext.equals("tif")) {
			BufferedImage bi=ImageIO.read(f);
			data=DBData.create(bi, ext);
			af=new File(f.getAbsolutePath().replace("."+ext, ".tfw"));
			ix.setTransform(PCUtil.loadTransform(af));
			ix.width=bi.getWidth();
			ix.height=bi.getHeight();
		}else if(ext.equals("geoson")) {
			
			
		}else {
			return new Pair(null,null);
		}

		ix.name=name;
		ix.type=type;
		ix.channel=1;
		ix.key=System.currentTimeMillis();
		data.key=ix.key;
		return new Pair(ix,data);
	}
	
	
	public static class Pair{
		Index index;
		DBData data;
		
		Pair(Index i,DBData d){
			index=i;
			data=d;
		}
	}
}
