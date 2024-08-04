package net.termat.tmgeo.util;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.awt.ShapeReader;
import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import com.mapbox.geojson.FeatureCollection;

public class VecUtil {
	private static ShapeWriter sw=new ShapeWriter();
	private static ShapeReader sr=new ShapeReader(new GeometryFactory());
	private static GeoJsonReader gr=new GeoJsonReader();
	private static GeoJsonWriter gw=new GeoJsonWriter();
	
	public static Area toShape(Geometry g) {
		return new Area(sw.toShape(g));
	}
	
	public static com.mapbox.geojson.Geometry toMapbox(Geometry g){
		String json=gw.write(g);
		String type=g.getGeometryType();
		if(type.equals("LineString")) {
			return com.mapbox.geojson.LineString.fromJson(json);
		}else if(type.equals("Point")) {
			return com.mapbox.geojson.Point.fromJson(json);
		}else if(type.equals("Polygon")) {
			return com.mapbox.geojson.Polygon.fromJson(json);
		}else if(type.equals("MultiLineString")) {
			return com.mapbox.geojson.MultiLineString.fromJson(json);
		}else if(type.equals("MultiPoint")) {
			return com.mapbox.geojson.MultiPoint.fromJson(json);
		}else if(type.equals("MultiPolygon")) {
			return com.mapbox.geojson.MultiPolygon.fromJson(json);
		}
		return null;
	}
	
	public static com.mapbox.geojson.Geometry toMapbox(Shape s){
		return toMapbox(toJTS(s));
	}
	
	public static Geometry toJTS(Shape s) {
		return sr.read(s.getPathIterator(AffineTransform.getScaleInstance(1.0, 1.0)));
	}
	
	public static Geometry toJTS(com.mapbox.geojson.Geometry g) throws ParseException {
		org.locationtech.jts.geom.Geometry geom=gr.read(g.toJson());
		return geom;
	}
	
	public static Geometry toJTS(org.gdal.ogr.Geometry g) throws ParseException {
		org.locationtech.jts.geom.Geometry geom=gr.read(g.ExportToJson());
		return geom;
	}
	
	public static org.gdal.ogr.Geometry toOGR(Geometry g){
		return org.gdal.ogr.Geometry.CreateFromJson(gw.write(g));
	}
	
	public static Area clip(Area a,Area b) {
		Area ret=new Area(a);
		ret.intersect(b);
		return ret;
	}
	
	public static Area union(Area a,Area b) {
		Area ret=new Area(a);
		ret.add(b);
		return ret;
	}
	
	public static Area sub(Area a,Area b) {
		Area ret=new Area(a);
		ret.subtract(b);
		return ret;
	}
	
	public static List<Area> toShapeMultiPolygon(MultiPolygon p){
		List<Area> ret=new ArrayList<>();
		for(int i=0;i<p.getNumGeometries();i++) {
			ret.add(new Area(sw.toShape(p.getGeometryN(i))));
		}
		return ret;
	}
	
	public static FeatureCollection loadFeatureCollection(File f) throws IOException {
		return FeatureCollection.fromJson(parse(f));
	}
	
	private static String parse(File f) throws IOException {
		StringBuffer buf=new StringBuffer();
		BufferedReader br=new BufferedReader(new FileReader(f));
		String line=null;
		while((line=br.readLine())!=null) {
			buf.append(line);
		}
		br.close();
		return buf.toString();
	}

}
