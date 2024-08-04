package net.termat.tmgeo.fomat.mbtiles;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.imintel.mbtiles4j.MBTilesWriteException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import net.termat.tmgeo.data.VectorReader;
import net.termat.tmgeo.fomat.mbtiles.MBHelperDB.Image;

public class MBTilesBuilder {
	public static String FORMAT_PNG="png";
	public static String FORMAT_JPG="jpg";
	public static String FORMAT_PBF="pbf";
	protected ConnectionSource connectionSource = null;
	protected Dao<metadata,Long> meta;
	protected Dao<tiles,Long> tiles;
	
	public static final String PARAM_ATTR="attribution";
	public static final String PARAM_CENTER="center";
	public static final String PARAM_FORMAT="format";
	public static final String PARAM_DESC="description";
	public static final String PARAM_BOUNDS="bounds";
	public static final String PARAM_MINZOOM="minzoom";
	public static final String PARAM_MAXZOOM="maxzoom";
	public static final String PARAM_NAME="name";

	private MBTilesBuilder() {}
	
	public void connectDB(String dbName,boolean create) throws SQLException{
		try{
			if(!dbName.endsWith(".hdb"))dbName=dbName+".hdb";
			Class.forName("org.sqlite.JDBC");
			connectionSource = new JdbcConnectionSource("jdbc:sqlite:"+dbName);
			tiles= DaoManager.createDao(connectionSource, tiles.class);
			meta= DaoManager.createDao(connectionSource, metadata.class);
			if(create)TableUtils.createTableIfNotExists(connectionSource, Image.class);
		}catch(SQLException e){
			e.printStackTrace();
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static MBTilesBuilder open(File f) throws SQLException {
		MBTilesBuilder ret=new MBTilesBuilder();
		ret.connectDB(f.getAbsolutePath(), true);
		return ret;
	}
	
	public static MBTilesBuilder open(File f,boolean isInit) throws SQLException {
		MBTilesBuilder ret=new MBTilesBuilder();
		ret.connectDB(f.getAbsolutePath(), isInit);
		return ret;
	}
	
	public String getMetadata(String name) throws SQLException {
		QueryBuilder<metadata, Long> query=meta.queryBuilder();
		query.where().eq("name",name);
		List<metadata> list=meta.query(query.prepare());
		if(list.size()==0) {
			return null;
		}else {
			return list.get(0).value;
		}
	}
	
	public boolean setMetadata(String name,String val) throws SQLException {
		QueryBuilder<metadata, Long> query=meta.queryBuilder();
		query.where().eq("name",name);
		List<metadata> list=meta.query(query.prepare());
		try {
			if(list.size()==0) {
				metadata m=new metadata();
				m.name=name;
				m.value=val;
				meta.create(m);
				return true;
			}else {
				metadata m=list.get(0);
				m.name=name;
				m.value=val;
				meta.update(m);
				return true;
			}
		}catch(SQLException ee){
			System.out.println(ee);
			return false;
		}
	}
	
	public boolean setBounds(Rectangle2D rect,int zoom) throws SQLException {
		QueryBuilder<metadata, Long> query=meta.queryBuilder();
		query.where().eq("name","bounds");
		List<metadata> list=meta.query(query.prepare());
		try {
			if(list.size()==0) {
				String rc=(float)rect.getX()+","+(float)rect.getY()+","+(rect.getX()+rect.getWidth())+","+(rect.getY()+rect.getHeight());
				metadata m=new metadata();
				m.name="bounds";
				m.value=rc;
				meta.create(m);
				m.name="center";
				m.value=(float)rect.getCenterX()+","+(float)rect.getCenterY()+","+zoom;
				meta.create(m);
				return true;
			}else {
				metadata m=list.get(0);
				m.value=unionBounds(rect);
				meta.update(m);
				query.where().eq("name","center");
				list=meta.query(query.prepare());
				m=list.get(0);
				m.value=(float)rect.getCenterX()+","+(float)rect.getCenterY()+","+zoom;
				meta.update(m);
				return true;
			}
		}catch(SQLException ee){
			System.out.println(ee);
			return false;
		}
	}
	
	public void createRasterTile(BufferedImage im,AffineTransform af) throws MBTilesWriteException, SQLException, IOException {
		MBTilesUtil.createMBTiles(null, im, af, getMinZoom(), getMaxZoom(), FORMAT_PNG);
	}
	
	public void addTile(tiles t) throws SQLException {
		tiles.create(t);
	}
	
	public List<tiles> getAllTiles() throws SQLException{
		return tiles.queryForAll();
	}
	
	public int getMinZoom() throws SQLException {
		QueryBuilder<metadata, Long> query=meta.queryBuilder();
		query.where().eq("name","minzoom");
		List<metadata> list=meta.query(query.prepare());
		if(list.size()==0) {
			return 0;
		}else {
			return Integer.parseInt(list.get(0).value);
		}
	}
	
	public int getMaxZoom() throws SQLException {
		QueryBuilder<metadata, Long> query=meta.queryBuilder();
		query.where().eq("name","maxzoom");
		List<metadata> list=meta.query(query.prepare());
		if(list.size()==0) {
			return 0;
		}else {
			return Integer.parseInt(list.get(0).value);
		}
	}
	
	public void updateJson(String json) throws SQLException {
		QueryBuilder<metadata, Long> query=meta.queryBuilder();
		query.where().eq("name","json");
		List<metadata> list=meta.query(query.prepare());
		if(list.size()==0) {
			metadata mm=new metadata();
			mm.name="json";
			mm.value=json;
			meta.create(mm);
		}else {
			metadata mm=list.get(0);
			Gson gson=new Gson();
			@SuppressWarnings("unchecked")
			Map<String,Object> map=(Map<String,Object>)gson.fromJson(mm.value, Map.class);
			@SuppressWarnings("unchecked")
			List<Object> li=(List<Object>)map.get("vector_layers");
			@SuppressWarnings("unchecked")
			Map<String,Object> map2=(Map<String,Object>)gson.fromJson(json, Map.class);
			@SuppressWarnings("unchecked")
			List<Object> li2=(List<Object>)map2.get("vector_layers");
			li.addAll(li2);
			Map<String,Object> mx=new HashMap<>();
			mx.put("vector_layers", li);
			mm.value=gson.toJson(mx);
			meta.update(mm);
		}
	}
	
	public void setJson(VectorReader vr,String layerName) throws SQLException {
		Item it=createJson(vr,layerName);
		Map<String,Object> root=new HashMap<>();
		List<Object> li=new ArrayList<>();
		li.add(it);
		root.put("vector_layers", li);
		Gson gson=new Gson();
		String json=gson.toJson(root);
		updateJson(json);
	}
	
	public static Item createJson(VectorReader vr,String layerName) {
		Item	 it=new Item();
		it.id=layerName;
		it.description="";
		JsonObject prop=vr.getPropertys().get(0);
		for(String key : prop.keySet()) {
			JsonPrimitive pp=prop.get(key).getAsJsonPrimitive();
			if(pp.isNumber()) {
				it.fields.put(key, "Number");
			}else if(pp.isString()) {
				it.fields.put(key, "String");
			}else {
				it.fields.put(key, "String");
			}
		}
		return it;
	}
	
	static class Item{
		String id;
		String description;
		Map<String,String> fields=new HashMap<>();
	}
	
	public static int yToRow(int zoom,int y) {
		return (int)Math.pow(2, zoom)-y;
	}
	
	public void addTile(int z,int x,int y,byte[] d) throws SQLException {
		tiles t=new tiles();
		t.zoom_level=z;
		t.tile_column=x;
		t.tile_row=yToRow(z,y);
		t.tile_data=d;
		addTile(t);
	}
	
	public tiles getTile(int z,int x,int y) throws SQLException {
		QueryBuilder<tiles, Long> query=tiles.queryBuilder();
		y=yToRow(z,y);
		query.where().eq("zoom_level",z).and().eq("tile_column", x).and().eq("tile_row", y);
		List<tiles> list=tiles.query(query.prepare());
		if(list.size()==0) {
			return null;
		}else {
			return list.get(0);
		}
	}
	
	public tiles getTileColRow(int z,int col,int row) throws SQLException {
		QueryBuilder<tiles, Long> query=tiles.queryBuilder();
		query.where().eq("zoom_level",z).and().eq("tile_column", col).and().eq("tile_row", row);
		List<tiles> list=tiles.query(query.prepare());
		if(list.size()==0) {
			return null;
		}else {
			return list.get(0);
		}
	}
	
	public void addTiles(List<tiles> t) throws SQLException {
		tiles.create(t);
	}
	
	private String unionBounds(Rectangle2D r) throws SQLException {
		QueryBuilder<metadata, Long> query=meta.queryBuilder();
		query.where().eq("name","bonds");
		List<metadata> list=meta.query(query.prepare());
		if(list.size()>0) {
			metadata mm=list.get(0);
			String[] ss=mm.value.split(",");
			if(ss.length==4) {
				Rectangle2D rc=new Rectangle2D.Double(
						Double.parseDouble(ss[0]),
						Double.parseDouble(ss[1]),
						Double.parseDouble(ss[2])-Double.parseDouble(ss[1]),
						Double.parseDouble(ss[3])-Double.parseDouble(ss[1]));
				rc=rc.createUnion(r);
				return (float)rc.getX()+","+(float)rc.getY()+","+(float)(rc.getX()+rc.getWidth())+","+(float)(rc.getY()+rc.getHeight());
			}else {
				return (float)r.getX()+","+(float)r.getY()+","+(float)(r.getX()+r.getWidth())+","+(float)(r.getY()+r.getHeight());
			}
		}else {
			return (float)r.getX()+","+(float)r.getY()+","+(float)(r.getX()+r.getWidth())+","+(float)(r.getY()+r.getHeight());
		}
	}
	
	
	
	
	public void updateTiles(tiles t) throws SQLException {
		tiles.update(t);
	}
	
	public static class tiles{
		public int zoom_level;
		public int tile_column;
		public int tile_row;
		public byte[] tile_data;
	}
	
	static class metadata{
		String  name;
		String value;
	}
	
}
